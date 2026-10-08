package uk.co.collom.parkping;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public final class ProviderThrottleTest {
    private static int checks;
    private static final long NOW = 1_800_000_000_000L;

    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }

    public static void main(String[] args) {
        ProviderThrottle throttle = new ProviderThrottle();
        check(throttle.delayMillis(NOW) == 0, "Initial request is immediately available");
        throttle.recordRequest(NOW);
        check(throttle.delayMillis(NOW) == ProviderThrottle.MIN_REFRESH_INTERVAL_MS,
                "A request starts the five-minute interval");
        check(throttle.delayMillis(NOW + ProviderThrottle.MIN_REFRESH_INTERVAL_MS - 1) == 1,
                "Requests remain blocked until the interval boundary");
        check(throttle.delayMillis(NOW + ProviderThrottle.MIN_REFRESH_INTERVAL_MS) == 0,
                "A request is available at the interval boundary");

        throttle.rateLimited("60", NOW + 1_000);
        check(throttle.delayMillis(NOW + 1_000) == ProviderThrottle.MIN_REFRESH_INTERVAL_MS,
                "Short Retry-After still respects the five-minute minimum");
        throttle.rateLimited("900", NOW + 1_000);
        check(throttle.delayMillis(NOW + 301_000) == 600_000,
                "Long Retry-After extends the backoff");
        throttle.recordSuccess();
        check(throttle.delayMillis(NOW + 301_000) == 0,
                "Success clears rate-limit backoff after the minimum interval");

        String httpDate = DateTimeFormatter.RFC_1123_DATE_TIME.format(
                Instant.ofEpochMilli(NOW + 600_000).atZone(java.time.ZoneOffset.UTC));
        throttle.rateLimited(httpDate, NOW);
        check(throttle.delayMillis(NOW + 300_000) == 300_000,
                "HTTP-date Retry-After is honored");
        ProviderThrottle restored = new ProviderThrottle();
        restored.restore(throttle.lastRequestAt(), throttle.retryAfterAt());
        check(restored.delayMillis(NOW + 300_000) == 300_000,
                "Persisted Retry-After backoff is restored");
        ProviderThrottle invalidHeader = new ProviderThrottle();
        invalidHeader.rateLimited("not-a-date", NOW);
        check(invalidHeader.delayMillis(NOW) == ProviderThrottle.MIN_REFRESH_INTERVAL_MS,
                "Invalid Retry-After uses safe minimum backoff");
        System.out.println("PASS: " + checks + " provider throttle checks");
    }
}
