package uk.co.collom.parkping;

import java.util.HashMap;
import java.util.Map;

/** Shares successful online responses between callers, never disk or outage fallback. */
final class SuccessfulSnapshotCache<T> {
    private record Entry<T>(T snapshot, long fetchedAt) { }
    private final Map<String, Entry<T>> snapshots = new HashMap<>();

    void remember(String parkId, T snapshot, long fetchedAt) {
        snapshots.put(parkId, new Entry<>(snapshot, fetchedAt));
    }

    T get(String parkId, long now, long retryAfterAt) {
        Entry<T> entry = snapshots.get(parkId);
        if (entry == null || retryAfterAt > now) return null;
        long age = now - entry.fetchedAt();
        if (age < 0 || age >= ProviderThrottle.MIN_REFRESH_INTERVAL_MS) {
            snapshots.remove(parkId);
            return null;
        }
        return entry.snapshot();
    }

    void invalidate(String parkId) { snapshots.remove(parkId); }
}
