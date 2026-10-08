package uk.co.collom.parkping;

/** Pure decision logic. Missing or stale observations never qualify for an alert. */
public final class AlertEngine {
    public static final long MAX_DATA_AGE_MS = 10 * 60_000L;
    public static final long MAX_LOCATION_AGE_MS = 2 * 60_000L;
    public record Rule(int maxWaitMinutes, int radiusMetres, boolean skipRidden,
                       boolean notifyReopening, int cooldownMinutes) {
        public Rule {
            if (maxWaitMinutes < 0 || radiusMetres < 100 || cooldownMinutes < 1) {
                throw new IllegalArgumentException("Invalid alert limits");
            }
        }
    }
    public record Observation(String status, Integer waitMinutes, double distanceMetres,
                              long updatedAt, long locationAgeMs, float accuracyMetres) { }
    public record Decision(boolean matches, boolean shouldNotify, String reason) { }
    private AlertEngine() { }
    public static Decision evaluate(Rule rule, Observation observation, boolean ridden,
                                    boolean previouslyMatched, String previousStatus,
                                    long lastNotificationAt, long now) {
        if (observation.updatedAt <= 0 || now - observation.updatedAt > MAX_DATA_AGE_MS
                || observation.updatedAt > now + 60_000L) return no("Queue data is stale or unavailable");
        if (observation.locationAgeMs < 0 || observation.locationAgeMs > MAX_LOCATION_AGE_MS
                || !Float.isFinite(observation.accuracyMetres) || observation.accuracyMetres > 100
                || observation.accuracyMetres < 0) return no("Waiting for a fresh, accurate location");
        if (!Double.isFinite(observation.distanceMetres) || observation.distanceMetres < 0
                || observation.distanceMetres > rule.radiusMetres) return no("Outside your distance limit");
        if (rule.skipRidden && ridden) return no("Already ridden today");
        if (!"OPERATING".equals(observation.status)) return no("Ride is not operating");
        boolean queueMatches = observation.waitMinutes != null && observation.waitMinutes >= 0
                && observation.waitMinutes <= rule.maxWaitMinutes;
        boolean reopening = rule.notifyReopening && "DOWN".equals(previousStatus);
        boolean cooledDown = lastNotificationAt <= 0
                || now - lastNotificationAt >= rule.cooldownMinutes * 60_000L;
        boolean notify = cooledDown && ((queueMatches && !previouslyMatched) || reopening);
        return new Decision(queueMatches, notify, reopening ? "Reopened nearby" : queueMatches
                ? (notify ? "Queue and distance match" : "Already notified or cooling down")
                : "Queue is above your limit");
    }
    private static Decision no(String reason) { return new Decision(false, false, reason); }
    public static double distanceMetres(double lat1, double lon1, double lat2, double lon2) {
        double a = Math.sin(Math.toRadians(lat2 - lat1) / 2);
        double b = Math.sin(Math.toRadians(lon2 - lon1) / 2);
        double h = a * a + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * b * b;
        return 6_371_000 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, h))));
    }
}
