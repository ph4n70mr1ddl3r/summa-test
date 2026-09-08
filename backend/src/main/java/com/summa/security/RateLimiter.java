package com.summa.security;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter {
    private final Map<String, Long> attemptCounts = new ConcurrentHashMap<>();
    private final Map<String, Instant> windowStarts = new ConcurrentHashMap<>();

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 60L;
    /**
     * Upper bound on tracked identifiers. Without eviction, an attacker can
     * flood distinct keys (e.g. random emails) and grow the maps without bound.
     */
    private static final int MAX_KEYS = 10_000;

    public boolean allow(String identifier) {
        purgeIfNeeded();
        Instant now = Instant.now();
        long windowStart = now.getEpochSecond() / WINDOW_SECONDS * WINDOW_SECONDS;

        Long count = attemptCounts.get(identifier);
        Instant window = windowStarts.get(identifier);

        if (window == null || window.getEpochSecond() != windowStart) {
            attemptCounts.put(identifier, 1L);
            windowStarts.put(identifier, Instant.ofEpochSecond(windowStart));
            return true;
        }

        if (count != null && count >= MAX_ATTEMPTS) {
            return false;
        }

        attemptCounts.put(identifier, (count != null ? count : 0L) + 1);
        return true;
    }

    public long getRemainingAttempts(String identifier) {
        Instant now = Instant.now();
        long windowStart = now.getEpochSecond() / WINDOW_SECONDS * WINDOW_SECONDS;

        Long count = attemptCounts.get(identifier);
        Instant window = windowStarts.get(identifier);

        if (window == null || window.getEpochSecond() != windowStart) {
            return MAX_ATTEMPTS;
        }

        long remaining = MAX_ATTEMPTS - (count != null ? count : 0L);
        return Math.max(0, remaining);
    }

    public void reset(String identifier) {
        attemptCounts.remove(identifier);
        windowStarts.remove(identifier);
    }

    /**
     * Drop entries whose window has expired once the map grows past the cap.
     * Cheap, lock-free, and good enough for a login rate limiter.
     */
    private void purgeIfNeeded() {
        if (attemptCounts.size() <= MAX_KEYS) {
            return;
        }
        Instant now = Instant.now();
        // Use a snapshot to avoid ConcurrentModificationException during removal.
        var keys = new ArrayList<String>(windowStarts.keySet());
        int removed = 0;
        for (String key : keys) {
            if (removed >= attemptCounts.size() - MAX_KEYS) break;
            Instant window = windowStarts.get(key);
            if (window != null && window.plusSeconds(WINDOW_SECONDS).isBefore(now)) {
                attemptCounts.remove(key);
                windowStarts.remove(key);
                removed++;
            }
        }
    }
}
