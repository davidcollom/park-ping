package uk.co.collom.parkping;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.ZoneId;

final class Store {
    private final SharedPreferences preferences;
    Store(Context context) { preferences = context.getSharedPreferences("park-ping", Context.MODE_PRIVATE); }
    int parkIndex() { return Math.max(0, Math.min(Models.PARKS.size() - 1, preferences.getInt("park", 0))); }
    Models.Park park() { return Models.PARKS.get(parkIndex()); }
    void setPark(int index) { preferences.edit().putInt("park", index).apply(); }
    boolean favourite(String id) { return preferences.getBoolean("fav-" + id, false); }
    void favourite(String id, boolean value) { preferences.edit().putBoolean("fav-" + id, value).apply(); }
    boolean ridden(String id) { return today().equals(preferences.getString("ridden-" + id, "")); }
    void toggleRidden(String id) { preferences.edit().putString("ridden-" + id, ridden(id) ? "" : today()).apply(); }
    private String today() { return LocalDate.now(ZoneId.of(park().timezone())).toString(); }
    AlertEngine.Rule rule(String id) {
        String saved = preferences.getString("rule-" + id, null);
        if (saved == null) return null;
        try {
            JSONObject j = new JSONObject(saved);
            return new AlertEngine.Rule(j.getInt("wait"), j.getInt("radius"),
                    j.getBoolean("skip"), j.getBoolean("reopen"), j.getInt("cooldown"));
        } catch (Exception ignored) { return null; }
    }
    void rule(String id, AlertEngine.Rule rule) {
        try {
            JSONObject j = new JSONObject();
            j.put("wait", rule.maxWaitMinutes()).put("radius", rule.radiusMetres())
                    .put("skip", rule.skipRidden()).put("reopen", rule.notifyReopening())
                    .put("cooldown", rule.cooldownMinutes());
            preferences.edit().putString("rule-" + id, j.toString()).apply();
            favourite(id, true);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    void removeRule(String id) { preferences.edit().remove("rule-" + id).apply(); }
    long lastPing(String id) { return preferences.getLong("ping-" + id, 0); }
    void ping(String id, long at) { preferences.edit().putLong("ping-" + id, at).apply(); }
}
