package uk.co.collom.parkping;

import java.util.HashMap;
import java.util.Map;

/** Per-park refresh cadence with a provider-wide HTTP 429 backoff. */
final class ProviderRequestGate {
    private final Map<String, ProviderThrottle> parks = new HashMap<>();
    private final ProviderThrottle providerBackoff = new ProviderThrottle();

    void restore(String parkId, long lastRequestAt, long retryAfterAt) {
        park(parkId).restore(lastRequestAt, 0);
        providerBackoff.restore(0, retryAfterAt);
    }

    long delayMillis(String parkId, long now) {
        return Math.max(park(parkId).delayMillis(now), providerBackoff.delayMillis(now));
    }

    void recordRequest(String parkId, long now) { park(parkId).recordRequest(now); }
    void rateLimited(String retryAfter, long now) { providerBackoff.rateLimited(retryAfter, now); }
    long lastRequestAt(String parkId) { return park(parkId).lastRequestAt(); }
    long retryAfterAt() { return providerBackoff.retryAfterAt(); }
    void recordSuccess() { providerBackoff.recordSuccess(); }

    private ProviderThrottle park(String id) {
        return parks.computeIfAbsent(id, ignored -> new ProviderThrottle());
    }
}
