package com.summa.security;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Per-identifier sliding-window rate limiter using atomic ConcurrentHashMap operations.
 * Single-process safe: no external store needed for the local JWT login gate.
 */
@Component
public class RateLimiter {
    private final Map<String, Long> attemptCounts = new ConcurrentHashMap<>();
    private final Map<String, Instant> windowStarts = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 60L;
    private static final int MAX_KEYS = 10_000;

    public boolean allow(String identifier) {
        purgeIfNeeded();
        Instant now = Instant.now();
        long windowStart = now.getEpochSecond() / WINDOW_SECONDS * WINDOW_SECONDS;

        // Use a per-key lock to make the window check + count update atomic.
        ReentrantLock lock = locks.computeIfAbsent(identifier, k -> new ReentrantLock());
        lock.lock();
        try {
            Instant window = windowStarts.get(identifier);
            if (window == null || window.getEpochSecond() != windowStart) {
                windowStarts.put(identifier, Instant.ofEpochSecond(windowStart));
                attemptCounts.put(identifier, 0L);
                return true;
            }
            long count = attemptCounts.getOrDefault(identifier, 0L) + 1L;
            long capped = Math.min(count, MAX_ATTEMPTS + 1L);
            attemptCounts.put(identifier, capped);
            return capped <= MAX_ATTEMPTS;
        } finally {
            lock.unlock();
        }
    }

    public long getRemainingAttempts(String identifier) {
        Instant now = Instant.now();
        long windowStart = now.getEpochSecond() / WINDOW_SECONDS * WINDOW_SECONDS;

        // Read without creating a lock entry — locks are only created in allow().
        Long count = attemptCounts.get(identifier);
        Instant window = windowStarts.get(identifier);
        if (window == null || window.getEpochSecond() != windowStart) {
            return MAX_ATTEMPTS;
        }
        long used = count != null ? count : 0L;
        // Count may be capped at MAX_ATTEMPTS+1 in allow(), so clamp to avoid negative remaining.
        return Math.max(0, MAX_ATTEMPTS - Math.min(used, MAX_ATTEMPTS));
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
        locks.remove(identifier);
    }

    private void purgeIfNeeded() {
        if (attemptCounts.size() <= MAX_KEYS) {
            return;
        }
        // Remove all entries whose window has expired to avoid unbounded growth.
        var keys = new ArrayList<String>(windowStarts.keySet());
        for (String key : keys) {
            Instant window = windowStarts.get(key);
            if (window != null && window.plusSeconds(WINDOW_SECONDS).isBefore(Instant.now())) {
                attemptCounts.remove(key);
                windowStarts.remove(key);
                locks.remove(key);
            }
        }
    }

    /**
     * Periodic purge invoked by a scheduled task to prevent memory leak under
     * steady-state unique-identifier load that never exceeds MAX_KEYS.
     */
    @jakarta.annotation.PostConstruct
    public void startPeriodicPurge() {
        // Use a daemon thread to periodically evict stale entries regardless of map size.
        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(WINDOW_SECONDS * 1000L);
                    purgeStaleEntries();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // best-effort purge — don't let exceptions propagate
                }
            }
        }, "summa-rate-limiter-purge");
        t.setDaemon(true);
        t.start();
    }

    private void purgeStaleEntries() {
        Instant now = Instant.now();
        var keys = new ArrayList<String>(windowStarts.keySet());
        for (String key : keys) {
            Instant window = windowStarts.get(key);
            if (window != null && window.plusSeconds(WINDOW_SECONDS).isBefore(now)) {
                attemptCounts.remove(key);
                windowStarts.remove(key);
                locks.remove(key);
            }
        }
    }
}
