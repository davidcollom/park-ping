package uk.co.collom.parkping;

public final class ProviderRequestGateTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        long now = 1_800_000_000_000L;
        ProviderRequestGate gate = new ProviderRequestGate();
        gate.restore("paris", 0, 0);
        gate.recordRequest("paris", now);
        check(gate.delayMillis("paris", now + 1) == 299_999,
                "UI and monitoring requests for the same park share cadence");
        check(gate.delayMillis("orlando", now + 1) == 0,
                "Switching to another park is not blocked by the previous park");
        gate.rateLimited("900", now);
        check(gate.delayMillis("orlando", now + 1) == 899_999,
                "Provider 429 blocks every park, including a previously unseen park");
        check(gate.delayMillis("paris", now + 1) == 899_999,
                "Provider backoff takes priority over the park cadence");
        ProviderRequestGate restarted = new ProviderRequestGate();
        restarted.restore("paris", gate.lastRequestAt("paris"), gate.retryAfterAt());
        check(restarted.delayMillis("paris", now + 300_000) == 600_000,
                "Persisted per-park cadence and global backoff survive restart");
        restarted.restore("orlando", 0, gate.retryAfterAt());
        check(restarted.delayMillis("orlando", now + 300_000) == 600_000,
                "Restoring a second park preserves shared provider backoff");
        check(restarted.delayMillis("orlando", now + 900_000) == 0,
                "Park switching resumes after the complete provider backoff");
        restarted.recordSuccess();
        check(restarted.delayMillis("paris", now + 900_000) == 0,
                "Success clears only an elapsed provider backoff");
        System.out.println("PASS: " + checks + " provider request gate checks");
    }
}
