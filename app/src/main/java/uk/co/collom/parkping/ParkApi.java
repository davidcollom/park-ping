package uk.co.collom.parkping;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/** HTTPS provider adapter with bounded timeouts, metadata caching and offline display. */
final class ParkApi {
    record Snapshot(List<Models.Ride> rides, boolean cached, String message) { }
    private static final Object REQUEST_LOCK = new Object();
    private static final ProviderRequestGate THROTTLE = new ProviderRequestGate();
    private final File directory;
    ParkApi(Context context) { directory = new File(context.getCacheDir(), "park-data"); directory.mkdirs(); }
    Snapshot load(Models.Park park) throws Exception {
        synchronized (REQUEST_LOCK) {
            File metadata = new File(directory, park.id() + "-children.json");
            File liveFile = new File(directory, park.id() + "-live.json");
            File lastRequest = new File(directory, park.id() + "-last-request");
            File retryAfter = new File(directory, "provider-retry-after");
            THROTTLE.restore(park.id(), readTimestamp(lastRequest), readTimestamp(retryAfter));
            long now = System.currentTimeMillis();
            JSONObject children = readRecent(metadata, now);
            JSONObject live = readRecent(liveFile, now);
            long delay = THROTTLE.delayMillis(park.id(), now);
            if (delay > 0) {
                if (children == null || live == null) throw new IOException("Park feed refresh is rate limited");
                return snapshot(children, live, true, "Refresh limited — showing cached data; alerts paused");
            }

            THROTTLE.recordRequest(park.id(), now);
            persistThrottle(park.id(), lastRequest, retryAfter);
            try {
                boolean metadataFresh = children != null;
                if (!metadataFresh) {
                    try {
                        children = request("/entity/" + park.id() + "/children");
                        write(metadata, children);
                    } catch (RateLimitException e) {
                        throw e;
                    } catch (Exception e) {
                        if (children == null) throw e;
                    }
                }
                live = request("/entity/" + park.id() + "/live");
                write(liveFile, live);
                THROTTLE.recordRequest(park.id(), System.currentTimeMillis());
                THROTTLE.recordSuccess();
                persistThrottle(park.id(), lastRequest, retryAfter);
                return snapshot(children, live, false, "Live feed refreshed");
            } catch (RateLimitException e) {
                long responseTime = System.currentTimeMillis();
                THROTTLE.rateLimited(e.retryAfter, responseTime);
                THROTTLE.recordRequest(park.id(), responseTime);
                persistThrottle(park.id(), lastRequest, retryAfter);
                if (children == null || live == null) throw e;
                return snapshot(children, live, true, "Provider rate limited requests — showing cached data; alerts paused");
            } catch (Exception e) {
                THROTTLE.recordRequest(park.id(), System.currentTimeMillis());
                persistThrottle(park.id(), lastRequest, retryAfter);
                if (children == null || live == null) throw e;
                return snapshot(children, live, true, "Offline — showing cached data; alerts paused");
            }
        }
    }
    private static Snapshot snapshot(JSONObject children, JSONObject live, boolean cached, String message) throws Exception {
        Map<String, JSONObject> locations = new HashMap<>();
        JSONArray entities = children.getJSONArray("children");
        for (int i = 0; i < entities.length(); i++) {
            JSONObject item = entities.getJSONObject(i);
            locations.put(item.getString("id"), item);
        }
        List<Models.Ride> rides = new ArrayList<>();
        JSONArray rows = live.getJSONArray("liveData");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject item = rows.getJSONObject(i);
            if (!"ATTRACTION".equals(item.optString("entityType"))) continue;
            JSONObject queue = item.optJSONObject("queue");
            JSONObject standby = queue == null ? null : queue.optJSONObject("STANDBY");
            Integer wait = standby == null || standby.isNull("waitTime") ? null : standby.getInt("waitTime");
            JSONObject entity = locations.get(item.getString("id"));
            JSONObject location = entity == null ? null : entity.optJSONObject("location");
            double lat = location == null ? Double.NaN : location.optDouble("latitude", Double.NaN);
            double lon = location == null ? Double.NaN : location.optDouble("longitude", Double.NaN);
            if (Math.abs(lat) > 90 || Math.abs(lon) > 180) { lat = Double.NaN; lon = Double.NaN; }
            long updated = 0;
            try { updated = Instant.parse(item.optString("lastUpdated")).toEpochMilli(); }
            catch (Exception ignored) { /* Missing freshness remains ineligible. */ }
            rides.add(new Models.Ride(item.getString("id"), item.getString("name"), lat, lon,
                    wait, item.optString("status", "UNKNOWN"), updated));
        }
        return new Snapshot(Collections.unmodifiableList(new ArrayList<>(rides)), cached, message);
    }
    private static long readTimestamp(File file) {
        if (!file.exists()) return 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            return Long.parseLong(reader.readLine());
        } catch (Exception ignored) { return 0; }
    }
    private static JSONObject readRecent(File file, long now) throws Exception {
        if (!file.exists()) return null;
        long age = now - file.lastModified();
        if (age < 0 || age >= ProviderThrottle.MIN_REFRESH_INTERVAL_MS) {
            file.delete();
            return null;
        }
        return read(file);
    }
    private void persistThrottle(String parkId, File lastRequest, File retryAfter) throws IOException {
        writeText(lastRequest, Long.toString(THROTTLE.lastRequestAt(parkId)));
        writeText(retryAfter, Long.toString(THROTTLE.retryAfterAt()));
    }
    private static void writeText(File target, String value) throws IOException {
        File temporary = File.createTempFile("throttle-", ".tmp", target.getParentFile());
        try {
            try (FileOutputStream out = new FileOutputStream(temporary)) {
                out.write(value.getBytes(StandardCharsets.UTF_8));
            }
            if (!temporary.renameTo(target)) throw new IOException("Could not persist provider throttle");
        } finally { temporary.delete(); }
    }
    private static final class RateLimitException extends IOException {
        private final String retryAfter;
        private RateLimitException(String retryAfter) {
            super("Park feed returned HTTP 429");
            this.retryAfter = retryAfter;
        }
    }
    private JSONObject request(String path) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("https://api.themeparks.wiki/v1" + path).openConnection();
        c.setConnectTimeout(12_000); c.setReadTimeout(15_000);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("User-Agent", "ParkPing/0.1 (personal Android prototype)");
        try {
            int code = c.getResponseCode();
            if (code == 429)
                throw new RateLimitException(c.getHeaderField("Retry-After"));
            if (code != 200) throw new IOException("Park feed returned HTTP " + code);
            try (InputStream in = c.getInputStream()) { return parse(in); }
        } finally { c.disconnect(); }
    }
    private static JSONObject read(File file) throws Exception {
        try (InputStream in = new FileInputStream(file)) { return parse(in); }
    }
    private static JSONObject parse(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192]; int n;
        while ((n = in.read(buffer)) != -1) {
            if (out.size() + n > 5_000_000) throw new IOException("Feed too large");
            out.write(buffer, 0, n);
        }
        return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
    }
    private static void write(File target, JSONObject data) throws IOException {
        File temporary = File.createTempFile("feed-", ".json", target.getParentFile());
        try {
            try (FileOutputStream out = new FileOutputStream(temporary)) {
                out.write(data.toString().getBytes(StandardCharsets.UTF_8));
            }
            if (!temporary.renameTo(target)) throw new IOException("Could not cache feed");
        } finally { temporary.delete(); }
    }
}
