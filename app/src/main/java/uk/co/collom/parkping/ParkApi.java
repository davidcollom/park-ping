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
    private final File directory;
    ParkApi(Context context) { directory = new File(context.getCacheDir(), "park-data"); directory.mkdirs(); }
    Snapshot load(Models.Park park) throws Exception {
        File metadata = new File(directory, park.id() + "-children.json");
        File liveFile = new File(directory, park.id() + "-live.json");
        JSONObject children;
        if (metadata.exists() && System.currentTimeMillis() - metadata.lastModified() < 86_400_000L) {
            children = read(metadata);
        } else {
            try { children = request("/entity/" + park.id() + "/children"); write(metadata, children); }
            catch (Exception e) { if (!metadata.exists()) throw e; children = read(metadata); }
        }
        JSONObject live;
        boolean cached = false;
        String message = "Live feed refreshed";
        try { live = request("/entity/" + park.id() + "/live"); write(liveFile, live); }
        catch (Exception e) {
            if (!liveFile.exists()) throw e;
            live = read(liveFile); cached = true;
            message = "Offline — showing cached data; alerts paused";
        }
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
    private JSONObject request(String path) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("https://api.themeparks.wiki/v1" + path).openConnection();
        c.setConnectTimeout(12_000); c.setReadTimeout(15_000);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("User-Agent", "ParkPing/0.1 (personal Android prototype)");
        try {
            int code = c.getResponseCode();
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
        // Different callers may refresh concurrently; each writes its own temporary file.
        File temporary = File.createTempFile("feed-", ".json", target.getParentFile());
        try {
            try (FileOutputStream out = new FileOutputStream(temporary)) {
                out.write(data.toString().getBytes(StandardCharsets.UTF_8));
            }
            if (!temporary.renameTo(target)) throw new IOException("Could not cache feed");
        } finally { temporary.delete(); }
    }
}
