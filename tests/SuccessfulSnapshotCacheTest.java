package uk.co.collom.parkping;

public final class SuccessfulSnapshotCacheTest {
    private static int checks;
    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }

    public static void main(String[] args) {
        long now = 1_800_000_000_000L;
        SuccessfulSnapshotCache<Object> cache = new SuccessfulSnapshotCache<>();
        Object online = new Object();
        check(cache.get("paris", now, 0) == null,
                "Restart has no successful online snapshot; disk fallback cannot become alert eligible");
        cache.remember("paris", online, now);
        check(cache.get("paris", now + 120_000, 0) == online,
                "Monitor shares a UI-fetched online response without another request");
        check(cache.get("paris", now + 240_000, 0) == online,
                "Further monitoring polls remain eligible within the response sharing window");
        check(cache.get("orlando", now + 120_000, 0) == null,
                "A successful snapshot is scoped to its own park");
        check(cache.get("paris", now + 120_000, now + 600_000) == null,
                "Provider-wide 429 suppresses shared online response alert eligibility");
        check(cache.get("paris", now + 300_000, 0) == null,
                "Response expires at the five-minute boundary");
        cache.remember("paris", online, now);
        cache.invalidate("paris");
        check(cache.get("paris", now + 1, 0) == null,
                "Beginning a new refresh invalidates the old response before an outage can occur");
        cache.remember("paris", online, now);
        check(cache.get("paris", now - 1, 0) == null,
                "Clock rollback cannot prolong online snapshot eligibility");
        System.out.println("PASS: " + checks + " successful snapshot sharing checks");
    }
}
