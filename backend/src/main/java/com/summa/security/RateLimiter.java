package com.summa.security;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-identifier sliding-window rate limiter using atomic ConcurrentHashMap operations.
 * Single-process safe: no external store needed for the local JWT login gate.
 */
@Component
public class RateLimiter {
    private final Map<String, Long> attemptCounts = new ConcurrentHashMap<>();
    private final Map<String, Instant> windowStarts = new ConcurrentHashMap<>();

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 60L;
    private static final int MAX_KEYS = 10_000;

    public boolean allow(String identifier) {
        purgeIfNeeded();
        Instant now = Instant.now();
        long windowStart = now.getEpochSecond() / WINDOW_SECONDS * WINDOW_SECONDS;

        // Atomic check-and-increment within a single window — decision returned from compute
        // eliminates the TOCTOU race between update and re-fetch of windowStarts
        final boolean[] allowed = new boolean[1];
        attemptCounts.compute(identifier, (key, count) -> {
            Instant window = windowStarts.get(key);
            if (window == null || window.getEpochSecond() != windowStart) {
                windowStarts.put(key, Instant.ofEpochSecond(windowStart));
                allowed[0] = true;
                return 1L;
            }
            long next = (count == null ? 0L : count) + 1L;
            long capped = Math.min(next, MAX_ATTEMPTS + 1L);
            allowed[0] = capped <= MAX_ATTEMPTS;
            return capped;
        });

        return allowed[0];
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

    public long getResetSeconds(String identifier) {
        Instant window = windowStarts.get(identifier);
        if (window == null) return 0;
        long resetAt = window.getEpochSecond() + WINDOW_SECONDS;
        long now = Instant.now().getEpochSecond();
        return Math.max(0, resetAt - now);
    }

    public void reset(String identifier) {
        attemptCounts.remove(identifier);
        windowStarts.remove(identifier);
    }

    private void purgeIfNeeded() {
        if (attemptCounts.size() <= MAX_KEYS) {
            return;
        }
        Instant now = Instant.now();
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
