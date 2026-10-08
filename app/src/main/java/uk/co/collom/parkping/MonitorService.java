package uk.co.collom.parkping;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.*;
import android.os.*;
import java.util.*;
import java.util.concurrent.*;

/** User-started location session: no background-location permission or boot restart. */
public final class MonitorService extends Service implements LocationListener {
    static volatile boolean active;
    static volatile android.location.Location latestLocation;
    static volatile String status = "Park mode is off";
    static final String SESSION_CHANNEL = "park-session";
    static final String ALERT_CHANNEL = "ride-alerts";
    private Store store;
    private LocationManager locations;
    private ScheduledExecutorService executor;
    private final Map<String, Boolean> matched = new HashMap<>();
    private final Map<String, String> previousStatus = new HashMap<>();
    private long started;
    private int failures;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Models.Park monitoredPark;
    static void channels(Context c) {
        NotificationManager n = c.getSystemService(NotificationManager.class);
        n.createNotificationChannel(new NotificationChannel(SESSION_CHANNEL, "Park mode", NotificationManager.IMPORTANCE_LOW));
        n.createNotificationChannel(new NotificationChannel(ALERT_CHANNEL, "Ride alerts", NotificationManager.IMPORTANCE_DEFAULT));
    }
    static boolean notificationsAllowed(Context c) {
        channels(c);
        NotificationManager n = c.getSystemService(NotificationManager.class);
        return (Build.VERSION.SDK_INT < 33 || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                && n.areNotificationsEnabled()
                && n.getNotificationChannel(ALERT_CHANNEL).getImportance() != NotificationManager.IMPORTANCE_NONE;
    }
    @Override public void onCreate() {
        super.onCreate(); store = new Store(this); channels(this);
        locations = getSystemService(LocationManager.class);
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (active) return START_NOT_STICKY;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf(); return START_NOT_STICKY;
        }
        if (!notificationsAllowed(this)) { status = "Enable ride notifications to start"; stopSelf(); return START_NOT_STICKY; }
        try {
            Notification notification = sessionNotification("Waiting for your location");
            if (Build.VERSION.SDK_INT >= 29) startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            else startForeground(1, notification);
            monitoredPark = store.park(); started = SystemClock.elapsedRealtime();
            latestLocation = null;
            for (String provider : java.util.Arrays.asList(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                if (locations.isProviderEnabled(provider)) {
                    locations.requestLocationUpdates(provider, 30_000L, 0f, this, Looper.getMainLooper());
                }
            }
            active = true; status = "Waiting for a fresh location";
            executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleWithFixedDelay(this::poll, 0, 120, TimeUnit.SECONDS);
        } catch (SecurityException | IllegalStateException e) {
            status = "Could not start Park mode; check location settings"; stopSelf();
        }
        return START_NOT_STICKY;
    }
    private Notification sessionNotification(String text) {
        Intent stop = new Intent(this, MonitorService.class).setAction("STOP");
        PendingIntent stopAction = PendingIntent.getService(this, 2, stop, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, SESSION_CHANNEL).setSmallIcon(R.drawable.ic_ping)
                .setContentTitle("Park Ping · Park mode on").setContentText(text).setOngoing(true)
                .setContentIntent(openApp()).addAction(new Notification.Action.Builder(null, "Stop Park mode", stopAction).build()).build();
    }
    private PendingIntent openApp() {
        return PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private void poll() {
        if (!active) return;
        if (SystemClock.elapsedRealtime() - started >= 12 * 60 * 60_000L) {
            status = "12-hour Park mode session ended"; main.post(this::stopSelf); return;
        }
        try {
            ParkApi.Snapshot snapshot = new ParkApi(this).load(monitoredPark);
            if (!active) return;
            if (snapshot.cached()) { status = snapshot.message(); updateSessionNotification(); return; }
            failures = 0;
            android.location.Location location = latestLocation;
            if (location == null) { status = "Waiting for a fresh location"; updateSessionNotification(); return; }
            long age = (SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos()) / 1_000_000;
            long now = System.currentTimeMillis(); int watched = 0, stale = 0;
            for (Models.Ride ride : snapshot.rides()) {
                AlertEngine.Rule rule = store.rule(ride.id());
                if (rule == null) continue;
                watched++;
                if (ride.updatedAt() <= 0 || now - ride.updatedAt() > AlertEngine.MAX_DATA_AGE_MS
                        || ride.updatedAt() > now + 60_000L) stale++;
                double distance = AlertEngine.distanceMetres(location.getLatitude(), location.getLongitude(), ride.latitude(), ride.longitude());
                AlertEngine.Observation observation = new AlertEngine.Observation(ride.status(), ride.waitMinutes(), distance,
                        ride.updatedAt(), age, location.hasAccuracy() ? location.getAccuracy() : Float.POSITIVE_INFINITY);
                AlertEngine.Decision decision = AlertEngine.evaluate(rule, observation, store.ridden(ride.id()),
                        matched.getOrDefault(ride.id(), false), previousStatus.get(ride.id()), store.lastPing(ride.id()), now);
                if (decision.shouldNotify() && active && notificationsAllowed(this)) {
                    Notification alert = new Notification.Builder(this, ALERT_CHANNEL)
                            .setSmallIcon(R.drawable.ic_ping).setContentTitle(ride.name())
                            .setContentText(decision.reason() + " · " + (ride.waitMinutes() == null ? "Posted wait unavailable" : ride.waitMinutes() + " min posted wait")
                                    + " · about " + Math.round(distance) + " m in a straight line")
                            .setContentIntent(openApp()).setAutoCancel(true).build();
                    getSystemService(NotificationManager.class).notify(ride.id().hashCode(), alert);
                    store.ping(ride.id(), now);
                }
                // Leave a crossing pending during cooldown; emit once when cooldown expires.
                if (!decision.matches() || decision.shouldNotify()) matched.put(ride.id(), decision.matches());
                if (ride.updatedAt() > 0 && now - ride.updatedAt() <= AlertEngine.MAX_DATA_AGE_MS)
                    previousStatus.put(ride.id(), ride.status());
            }
            status = age > AlertEngine.MAX_LOCATION_AGE_MS ? "Location is stale — alerts paused"
                    : !location.hasAccuracy() || location.getAccuracy() > 100 ? "Location too approximate — alerts paused"
                    : stale == 0 ? "Watching " + watched + " rides · checked just now"
                    : stale == watched ? "Wait estimates are stale — alerts paused"
                    : "Some wait estimates are stale — alerts paused for those rides";
            updateSessionNotification();
        } catch (Exception e) {
            failures++; status = "Park feed unavailable — retrying; alerts paused";
            updateSessionNotification();
            // A bounded extra delay backs off repeated network failures; interrupted on stop.
            if (failures > 1) {
                try { Thread.sleep(Math.min(600_000L, failures * 60_000L)); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }
        }
    }
    private void updateSessionNotification() {
        getSystemService(NotificationManager.class).notify(1, sessionNotification(status));
    }
    @Override public void onLocationChanged(android.location.Location location) { latestLocation = new android.location.Location(location); }
    @Override public void onProviderEnabled(String provider) { }
    @Override public void onProviderDisabled(String provider) { status = "Location provider disabled"; }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() {
        active = false; latestLocation = null;
        if (executor != null) executor.shutdownNow();
        if (locations != null) locations.removeUpdates(this);
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
}
