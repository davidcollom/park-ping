package uk.co.collom.parkping;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Native Android UI for the approved phone prototype. */
public final class MainActivity extends Activity {
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private Store store;
    private LinearLayout screen, content;
    private TextView feedStatus, sessionStatus;
    private Button sessionButton;
    private List<Models.Ride> rides = new ArrayList<>();
    private boolean loading;
    private boolean cached;
    private String page = "Nearby";
    private int generation;
    private long refreshed;
    private int background, ink, muted, card, line, accent, onAccent, purple, onPurple, soft;
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            updateSession();
            if (!loading && SystemClock.elapsedRealtime() - refreshed > 120_000L) refresh();
            main.postDelayed(this, 5_000);
        }
    };
    @Override public void onCreate(Bundle state) {
        boolean dark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        setTheme(dark ? R.style.AppThemeDark : R.style.AppTheme);
        super.onCreate(state); store = new Store(this);
        colours(); MonitorService.channels(this); build(); refresh();
    }
    private void colours() {
        boolean dark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        background = Color.parseColor(dark ? "#11131E" : "#F7F7FC");
        card = Color.parseColor(dark ? "#1C2030" : "#FFFFFF");
        ink = Color.parseColor(dark ? "#EDF0FF" : "#202238");
        muted = Color.parseColor(dark ? "#B3BBCE" : "#62657A");
        line = Color.parseColor(dark ? "#353D53" : "#E1E2ED");
        accent = Color.parseColor(dark ? "#72D8C7" : "#08786F");
        onAccent = Color.parseColor(dark ? "#102E2B" : "#FFFFFF");
        purple = accent;
        onPurple = onAccent;
        soft = Color.parseColor(dark ? "#203B3A" : "#E3F5F0");
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private GradientDrawable surface(int colour, int radius, boolean border) {
        GradientDrawable d = new GradientDrawable(); d.setColor(colour); d.setCornerRadius(dp(radius));
        if (border) d.setStroke(dp(1), line); return d;
    }
    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout horizontal() { LinearLayout l = new LinearLayout(this); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(ink);
        if (bold) t.setTypeface(null, Typeface.BOLD); t.setPadding(0, dp(3), 0, dp(3)); return t;
    }
    private TextView note(String value) { TextView t = text(value, 13, false); t.setTextColor(muted); return t; }
    private Button button(String title, Runnable click, boolean primary) {
        Button b = new Button(this); b.setText(title); b.setAllCaps(false); b.setTextSize(14);
        b.setMinHeight(dp(48)); b.setMinimumWidth(0); b.setPadding(dp(12), dp(5), dp(12), dp(5));
        b.setTextColor(primary ? onAccent : accent); b.setBackground(surface(primary ? accent : soft, 12, false));
        b.setOnClickListener(v -> click.run()); return b;
    }
    private void addGap(LinearLayout parent, int height) { View v = new View(this); parent.addView(v, new LinearLayout.LayoutParams(1, dp(height))); }
    private void weighted(LinearLayout row, View view) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        p.setMargins(dp(3), dp(3), dp(3), dp(3)); row.addView(view, p);
    }
    private void build() {
        screen = vertical(); screen.setBackgroundColor(background); screen.setPadding(dp(18), dp(8), dp(18), 0);
        screen.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(dp(18) + bars.left, dp(8) + bars.top, dp(18) + bars.right, bars.bottom);
            } else v.setPadding(dp(18), dp(8) + insets.getSystemWindowInsetTop(), dp(18), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(screen);
        LinearLayout brand = horizontal();
        ImageView mascot = new ImageView(this); mascot.setImageResource(R.drawable.park_ping_mascot);
        mascot.setContentDescription("Park Ping smiling location-pin mascot");
        mascot.setScaleType(ImageView.ScaleType.FIT_CENTER);
        brand.addView(mascot, new LinearLayout.LayoutParams(dp(64), dp(64)));
        LinearLayout wordmark = vertical(); wordmark.setPadding(dp(10), 0, 0, 0);
        wordmark.addView(text("Park Ping", 28, true));
        wordmark.addView(note("A shorter queue. Just around the corner."));
        brand.addView(wordmark, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        screen.addView(brand); addGap(screen, 8);
        Spinner parks = new Spinner(this);
        ArrayAdapter<Models.Park> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Models.PARKS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); parks.setAdapter(adapter); parks.setSelection(store.parkIndex());
        screen.addView(parks, new LinearLayout.LayoutParams(-1, dp(48)));
        parks.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> p) { }
            @Override public void onItemSelected(AdapterView<?> p, View v, int position, long id) {
                if (position == store.parkIndex()) return;
                stopService(new Intent(MainActivity.this, MonitorService.class));
                store.setPark(position); generation++; rides = new ArrayList<>(); refreshed = 0; loading = false;
                render(); refresh(); updateSession();
            }
        });
        LinearLayout session = vertical(); session.setPadding(dp(12), dp(10), dp(12), dp(10)); session.setBackground(surface(soft, 16, false));
        sessionStatus = text("Park mode is off", 14, true); session.addView(sessionStatus);
        session.addView(note("Location stays on your phone. Stop whenever you like."));
        sessionButton = button("Start Park mode", this::toggleSession, false); session.addView(sessionButton); screen.addView(session);
        ScrollView scroll = new ScrollView(this); content = vertical(); scroll.addView(content);
        screen.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout navigation = horizontal();
        for (String tab : java.util.Arrays.asList("Nearby", "Favourites", "My alerts")) {
            weighted(navigation, button(tab, () -> { page = tab; render(); }, false));
        }
        screen.addView(navigation); updateSession();
    }
    private void updateSession() {
        if (sessionStatus == null) return;
        sessionButton.setText(MonitorService.active ? "Stop Park mode" : "Start Park mode");
        sessionStatus.setText(MonitorService.active ? MonitorService.status : "Park mode is off");
    }
    private void toggleSession() {
        if (MonitorService.active) { stopService(new Intent(this, MonitorService.class)); main.postDelayed(this::updateSession, 200); return; }
        new AlertDialog.Builder(this).setTitle("Enable Park mode?")
                .setMessage("Park Ping will check your location and live queues about every two minutes, including with the screen off. A persistent notification lets you stop it. Location is used on this device and is not uploaded. Park mode ends after 12 hours. Android power saving can delay checks.")
                .setNegativeButton("Cancel", null).setPositiveButton("Continue", (d, w) -> permissionsAndStart()).show();
    }
    private void permissionsAndStart() {
        List<String> needed = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION); needed.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.POST_NOTIFICATIONS);
        if (!needed.isEmpty()) { requestPermissions(needed.toArray(new String[0]), 10); return; }
        startSession();
    }
    private void startSession() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            message("Location permission is needed for nearby alerts. You can still browse rides."); return;
        }
        if (!MonitorService.notificationsAllowed(this)) {
            new AlertDialog.Builder(this).setTitle("Notifications are switched off")
                    .setMessage("Enable Park Ping’s ride alerts in Android settings before starting Park mode.")
                    .setNegativeButton("Later", null).setPositiveButton("Settings", (d,w) -> startActivity(
                            new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()))).show(); return;
        }
        LocationManager manager = getSystemService(LocationManager.class);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            message("Switch on your phone’s location services first."); return;
        }
        if (rides.stream().noneMatch(r -> store.rule(r.id()) != null)) { message("Set at least one ride alert before starting Park mode."); return; }
        try { startForegroundService(new Intent(this, MonitorService.class)); main.postDelayed(this::updateSession, 300); }
        catch (RuntimeException e) { message("Android could not start Park mode. Please try again from this screen."); }
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 10) startSession();
        if (code == 11) sendTestNotification();
    }
    private void requestTestNotification() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11); return;
        }
        sendTestNotification();
    }
    private void sendTestNotification() {
        if (!MonitorService.notificationsAllowed(this)) { message("Enable ride notifications in Android settings to send a test."); return; }
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        getSystemService(NotificationManager.class).notify(100,
                new Notification.Builder(this, MonitorService.ALERT_CHANNEL).setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Park Ping · Test notification")
                        .setContentText("Example only: a favourite ride has a 15 min queue, 250 m away.")
                        .setContentIntent(open).setAutoCancel(true).build());
        message("Test notification sent — this is sample data.");
    }
    private void message(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private void refresh() {
        if (loading || isFinishing()) return; loading = true;
        int requestGeneration = generation; Models.Park park = store.park(); render();
        network.execute(() -> {
            try {
                ParkApi.Snapshot snapshot = new ParkApi(this).load(park);
                main.post(() -> {
                    if (requestGeneration != generation || isDestroyed()) return;
                    rides = snapshot.rides(); cached = snapshot.cached(); loading = false;
                    refreshed = SystemClock.elapsedRealtime(); render();
                    feedStatus.setText(snapshot.message() + " · " + rides.size() + " rides");
                });
            } catch (Exception e) {
                main.post(() -> {
                    if (requestGeneration != generation || isDestroyed()) return;
                    loading = false; refreshed = SystemClock.elapsedRealtime(); render();
                    feedStatus.setText("Could not load park data. Check your connection and tap Refresh.");
                });
            }
        });
    }
    private String age(Models.Ride r) {
        if (r.updatedAt() <= 0) return "Update time unavailable";
        long minutes = Math.max(0, (System.currentTimeMillis() - r.updatedAt()) / 60_000);
        return "Updated " + (minutes < 1 ? "just now" : minutes + " min ago")
                + (minutes > 10 ? " · stale, no alerts" : "") + (cached ? " · cached" : "");
    }
    private Double distance(Models.Ride r) {
        android.location.Location l = MonitorService.latestLocation;
        if (l == null || (SystemClock.elapsedRealtimeNanos() - l.getElapsedRealtimeNanos()) / 1_000_000 > AlertEngine.MAX_LOCATION_AGE_MS) return null;
        double d = AlertEngine.distanceMetres(l.getLatitude(), l.getLongitude(), r.latitude(), r.longitude());
        return Double.isFinite(d) ? d : null;
    }
    private void render() {
        if (content == null) return; content.removeAllViews(); addGap(content, 12);
        content.addView(text(page.equals("Nearby") ? "Near you" : page.equals("Favourites") ? "Your favourites" : "My alerts", 21, true));
        feedStatus = note(loading ? "Loading live park data…" : "Data from ThemeParks.wiki · Posted waits may change"); content.addView(feedStatus);
        Button refresh = button(loading ? "Refreshing…" : "Refresh", this::refresh, false); refresh.setEnabled(!loading); content.addView(refresh);
        List<Models.Ride> visible = new ArrayList<>();
        for (Models.Ride r : rides) {
            if (page.equals("Favourites") && !store.favourite(r.id())) continue;
            if (page.equals("My alerts") && store.rule(r.id()) == null) continue;
            visible.add(r);
        }
        visible.sort(Comparator.comparing((Models.Ride r) -> !store.favourite(r.id()))
                .thenComparingDouble(r -> distance(r) == null ? Double.POSITIVE_INFINITY : distance(r)).thenComparing(Models.Ride::name));
        if (visible.isEmpty() && !loading) {
            if (page.equals("My alerts")) emptyState("Your alerts start here",
                    "Choose Set alert on a ride to create your first rule.");
            else if (page.equals("Favourites")) emptyState("Your favourites are waiting",
                    "Favourite a ride from Nearby to keep it here.");
            else emptyState("Your park day is just getting started",
                    "No ride data is available yet. Try refreshing in a moment.");
        }
        if (page.equals("Nearby") && MonitorService.latestLocation == null) content.addView(note("Start Park mode for nearby distances. You can set alerts first."));
        for (Models.Ride r : visible) rideCard(r);
        addGap(content, 16);
        TextView attribution = note("Data: ThemeParks.wiki ↗ · Unofficial app, not affiliated with Disney or Universal.");
        attribution.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://themeparks.wiki")))); content.addView(attribution);
    }
    private void rideCard(Models.Ride r) {
        addGap(content, 12); LinearLayout c = vertical(); c.setPadding(dp(14), dp(14), dp(14), dp(14)); c.setBackground(surface(card, 18, true));
        c.addView(text(r.name(), 17, true));
        String wait = "OPERATING".equals(r.status()) ? (r.waitMinutes() == null ? "Queue unavailable" : r.waitMinutes() + " min queue") : r.status().toLowerCase(Locale.UK);
        Double distance = distance(r);
        c.addView(text(wait + (distance == null ? "" : " · " + Math.round(distance) + " m away"), 16, true)); c.addView(note(age(r)));
        if (store.ridden(r.id())) c.addView(note("✓ Ridden today"));
        AlertEngine.Rule q = store.rule(r.id());
        if (q != null) c.addView(note("Alert: queue ≤ " + q.maxWaitMinutes() + " min · Within " + q.radiusMetres() + " m"));
        LinearLayout actions = horizontal();
        weighted(actions, button(store.favourite(r.id()) ? "♥ Saved" : "♡ Favourite", () -> { store.favourite(r.id(), !store.favourite(r.id())); render(); }, false));
        weighted(actions, button(q == null ? "Set alert" : "Edit alert", () -> edit(r), false)); c.addView(actions);
        c.addView(button(store.ridden(r.id()) ? "Undo ridden today" : "Mark ridden today", () -> { store.toggleRidden(r.id()); render(); }, false));
        content.addView(c);
    }
    private void emptyState(String title, String description) {
        LinearLayout state = horizontal(); state.setPadding(dp(14), dp(12), dp(14), dp(12));
        state.setBackground(surface(card, 18, true));
        ImageView mascot = new ImageView(this); mascot.setImageResource(R.drawable.park_ping_mascot);
        mascot.setContentDescription(null); mascot.setScaleType(ImageView.ScaleType.FIT_CENTER);
        state.addView(mascot, new LinearLayout.LayoutParams(dp(68), dp(68)));
        LinearLayout copy = vertical(); copy.setPadding(dp(12), 0, 0, 0);
        copy.addView(text(title, 16, true)); copy.addView(note(description));
        state.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        content.addView(state);
    }
    private void edit(Models.Ride r) {
        AlertEngine.Rule saved = store.rule(r.id());
        AlertEngine.Rule q = saved == null ? new AlertEngine.Rule(20, 750, true, true, 30) : saved;
        ScrollView scroll = new ScrollView(this); LinearLayout fields = vertical(); fields.setPadding(dp(20), dp(8), dp(20), dp(12)); scroll.addView(fields);
        SeekBar wait = slider(fields, "Maximum queue", q.maxWaitMinutes(), 0, 90, 5, " min");
        SeekBar radius = slider(fields, "Within this distance", q.radiusMetres(), 100, 2000, 50, " m");
        Switch skip = new Switch(this); skip.setText("Only if I haven’t ridden it today"); skip.setChecked(q.skipRidden()); skip.setMinHeight(dp(56)); fields.addView(skip);
        Switch reopen = new Switch(this); reopen.setText("Also alert when it reopens nearby"); reopen.setChecked(q.notifyReopening()); reopen.setMinHeight(dp(56)); fields.addView(reopen);
        fields.addView(button("Send test notification", this::requestTestNotification, false));
        fields.addView(note("Time between alerts")); Spinner cooldown = new Spinner(this);
        cooldown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, java.util.Arrays.asList("15 minutes", "30 minutes", "1 hour")));
        cooldown.setSelection(q.cooldownMinutes() == 15 ? 0 : q.cooldownMinutes() == 60 ? 2 : 1); fields.addView(cooldown);
        fields.addView(note("Distance is straight-line, not walking time. Reopening alerts respect distance, ridden status and cooldown; they can trigger above your queue limit."));
        new AlertDialog.Builder(this).setTitle(r.name()).setView(scroll).setNegativeButton("Cancel", null)
                .setNeutralButton(saved == null ? "" : "Remove", (dialog, which) -> { store.removeRule(r.id()); render(); })
                .setPositiveButton("Save alert", (dialog, which) -> {
                    int minutes = new int[]{15, 30, 60}[cooldown.getSelectedItemPosition()];
                    store.rule(r.id(), new AlertEngine.Rule(wait.getProgress() * 5, 100 + radius.getProgress() * 50, skip.isChecked(), reopen.isChecked(), minutes));
                    render(); message("Alert saved. Start Park mode to receive nearby alerts.");
                }).show();
    }
    private SeekBar slider(LinearLayout fields, String title, int value, int minimum, int maximum, int step, String unit) {
        TextView label = text(title + ": " + value + unit, 15, true); fields.addView(label);
        SeekBar bar = new SeekBar(this); bar.setMax((maximum - minimum) / step); bar.setProgress((value - minimum) / step);
        fields.addView(bar, new LinearLayout.LayoutParams(-1, dp(48)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar b, int p, boolean user) { label.setText(title + ": " + (minimum + p * step) + unit); }
            @Override public void onStartTrackingTouch(SeekBar b) { }
            @Override public void onStopTrackingTouch(SeekBar b) { }
        }); return bar;
    }
    @Override protected void onResume() {
        super.onResume();
        if (MonitorService.active
                && ((checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                || !MonitorService.notificationsAllowed(this))) {
            stopService(new Intent(this, MonitorService.class));
            main.postDelayed(this::updateSession, 200);
        }
        main.post(ticker);
    }
    @Override protected void onPause() { main.removeCallbacks(ticker); super.onPause(); }
    @Override protected void onDestroy() { generation++; network.shutdownNow(); main.removeCallbacksAndMessages(null); super.onDestroy(); }
}
