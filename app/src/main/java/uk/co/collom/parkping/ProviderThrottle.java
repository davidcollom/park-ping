package uk.co.collom.parkping;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

final class ProviderThrottle {
    static final long MIN_REFRESH_INTERVAL_MS = 5 * 60_000L;
    private long lastRequestAt;
    private long retryAfterAt;

    long delayMillis(long now) {
        long nextAllowed = Math.max(addSaturated(lastRequestAt, MIN_REFRESH_INTERVAL_MS), retryAfterAt);
        return nextAllowed <= now ? 0 : nextAllowed - now;
    }

    void restore(long lastRequestAt, long retryAfterAt) {
        this.lastRequestAt = Math.max(0, lastRequestAt);
        this.retryAfterAt = Math.max(0, retryAfterAt);
    }

    void recordRequest(long now) {
        lastRequestAt = now;
    }

    void rateLimited(String retryAfter, long now) {
        long minimumBackoff = addSaturated(now, MIN_REFRESH_INTERVAL_MS);
        retryAfterAt = Math.max(retryAfterAt, Math.max(minimumBackoff, retryAfterAt(retryAfter, now)));
    }

    void recordSuccess() {
        retryAfterAt = 0;
    }

    long retryAfterAt() {
        return retryAfterAt;
    }

    long lastRequestAt() {
        return lastRequestAt;
    }

    private static long retryAfterAt(String value, long now) {
        if (value == null || value.isBlank()) return now;
        String trimmed = value.trim();
        try {
            long seconds = Long.parseLong(trimmed);
            if (seconds < 0) return now;
            return addSaturated(now, multiplySaturated(seconds, 1000));
        } catch (NumberFormatException ignored) {
            try {
                return ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant().toEpochMilli();
            } catch (RuntimeException ignoredDate) {
                return now;
            }
        }
    }

    private static long addSaturated(long a, long b) {
        return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b;
    }

    private static long multiplySaturated(long a, long b) {
        return a > Long.MAX_VALUE / b ? Long.MAX_VALUE : a * b;
    }
}
