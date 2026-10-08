package uk.co.collom.parkping;

public final class AlertEngineTest {
    private static int checks;
    private static final long NOW = 1_800_000_000_000L;
    private static final AlertEngine.Rule RULE = new AlertEngine.Rule(20, 750, true, true, 30);
    private static AlertEngine.Observation observation(String status, Integer wait, double distance,
                                                       long updatedAt, long locationAge, float accuracy) {
        return new AlertEngine.Observation(status, wait, distance, updatedAt, locationAge, accuracy);
    }
    private static AlertEngine.Decision evaluate(AlertEngine.Observation o, boolean ridden,
                                                 boolean matched, String status, long last) {
        return AlertEngine.evaluate(RULE, o, ridden, matched, status, last, NOW);
    }
    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        var good = observation("OPERATING", 20, 750, NOW, 0, 15);
        check(evaluate(good, false, false, null, 0).shouldNotify(), "Inclusive limits trigger");
        check(!evaluate(good, true, false, null, 0).shouldNotify(), "Ridden suppresses");
        check(!evaluate(good, false, true, "OPERATING", 0).shouldNotify(), "No repeated steady alerts");
        check(!evaluate(good, false, false, null, NOW - 1000).shouldNotify(), "Cooldown suppresses");
        check(evaluate(good, false, false, null, NOW - 1_800_000).shouldNotify(), "Cooldown boundary");
        check(!evaluate(observation("OPERATING", 21, 300, NOW, 0, 10), false, false, null, 0).shouldNotify(), "Queue above target");
        check(!evaluate(observation("OPERATING", null, 300, NOW, 0, 10), false, false, null, 0).shouldNotify(), "Missing wait is not zero");
        check(!evaluate(observation("OPERATING", -1, 300, NOW, 0, 10), false, false, null, 0).shouldNotify(), "Negative wait rejected");
        check(!evaluate(observation("OPERATING", 0, 751, NOW, 0, 10), false, false, null, 0).shouldNotify(), "Distance outside");
        check(!evaluate(observation("OPERATING", 0, Double.NaN, NOW, 0, 10), false, false, null, 0).shouldNotify(), "Missing coordinates");
        check(!evaluate(observation("OPERATING", 0, 300, NOW - 600001, 0, 10), false, false, null, 0).shouldNotify(), "Stale queue rejected");
        check(!evaluate(observation("OPERATING", 0, 300, 0, 0, 10), false, false, null, 0).shouldNotify(), "Missing timestamp");
        check(!evaluate(observation("OPERATING", 0, 300, NOW, 120001, 10), false, false, null, 0).shouldNotify(), "Old location rejected");
        check(!evaluate(observation("OPERATING", 0, 300, NOW, 0, 101), false, false, null, 0).shouldNotify(), "Inaccurate location");
        check(!evaluate(observation("DOWN", 0, 300, NOW, 0, 10), false, false, null, 0).shouldNotify(), "Down is not open");
        check(evaluate(observation("OPERATING", 60, 300, NOW, 0, 10), false, false, "DOWN", 0).shouldNotify(), "Reopening independently triggers");
        check(!evaluate(observation("OPERATING", 60, 300, NOW, 0, 10), false, false, "CLOSED", 0).shouldNotify(), "Morning opening is not reopening");
        check(!evaluate(observation("OPERATING", 60, 800, NOW, 0, 10), false, false, "DOWN", 0).shouldNotify(), "Reopening respects proximity");
        check(!evaluate(observation("OPERATING", 60, 300, NOW, 0, 10), true, false, "DOWN", 0).shouldNotify(), "Reopening respects ridden");
        check(!evaluate(observation("OPERATING", 60, 300, NOW, 0, 10), false, false, "DOWN", NOW).shouldNotify(), "Reopening respects cooldown");
        check(AlertEngine.distanceMetres(28.4, -81.5, 28.4, -81.5) < .001, "Same point");
        check(Math.abs(AlertEngine.distanceMetres(0, 0, 0, 1) - 111195) < 2, "Distance sanity");
        check(!evaluate(observation("OPERATING", 0, 300, NOW + 61_000, 0, 10), false, false, null, 0).shouldNotify(), "Future feed timestamp rejected");
        check(!evaluate(observation("OPERATING", 0, 300, NOW, -1, 10), false, false, null, 0).shouldNotify(), "Invalid location age");
        check(!evaluate(observation("OPERATING", 0, 300, NOW, 0, Float.NaN), false, false, null, 0).shouldNotify(), "Invalid accuracy");
        check(AlertEngine.isFreshData(NOW - 600_000, NOW), "Ten-minute boundary remains fresh");
        check(!AlertEngine.isFreshData(NOW - 600_001, NOW), "One millisecond beyond cutoff is stale");
        check(AlertEngine.isFreshData(NOW + 60_000, NOW), "One-minute future allowance is inclusive");
        String remembered = AlertEngine.statusForHistory(null, "DOWN", NOW + 60_001, NOW);
        check(remembered == null, "Future DOWN never seeds reopening history");
        check(!evaluate(observation("OPERATING", 60, 300, NOW, 0, 10), false, false, remembered, 0).shouldNotify(),
                "Valid operating data after future DOWN cannot falsely reopen");
        check("OPERATING".equals(AlertEngine.statusForHistory("OPERATING", "DOWN", NOW - 600_001, NOW)),
                "Stale DOWN does not replace valid history");
        var disabledReopen = new AlertEngine.Rule(20, 750, true, false, 30);
        check(!AlertEngine.evaluate(disabledReopen, observation("OPERATING", 60, 300, NOW, 0, 10),
                false, false, "DOWN", 0, NOW).shouldNotify(), "Reopening toggle honoured");
        System.out.println("PASS: " + checks + " alert decision checks");
    }
}
