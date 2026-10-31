package com.summa.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    private RateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new RateLimiter();
    }

    @Test
    void allow_firstRequest_alwaysAllowed() {
        assertTrue(limiter.allow("user-1"));
    }

    @Test
    void allow_withinLimit_allows() {
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.allow("user-2"), "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void allow_exceedsLimit_rejects() {
        for (int i = 0; i < 5; i++) {
            limiter.allow("user-3");
        }
        assertFalse(limiter.allow("user-3"));
    }

    @Test
    void allow_exactlyMaxAttemptsAllowed_sixthRejected() {
        boolean[] results = new boolean[7];
        for (int i = 0; i < results.length; i++) {
            results[i] = limiter.allow("user-limit");
        }
        assertTrue(results[0]);
        assertTrue(results[1]);
        assertTrue(results[2]);
        assertTrue(results[3]);
        assertTrue(results[4], "Fifth request must be allowed");
        assertFalse(results[5], "Sixth request must be rejected");
        assertFalse(results[6], "Seventh request must be rejected");
    }

    @Test
    void getRemainingAttempts_rejectsAfterExactlyMaxAttempts() {
        for (int i = 0; i < 5; i++) {
            limiter.allow("user-exact");
        }
        assertEquals(0, limiter.getRemainingAttempts("user-exact"));
        assertFalse(limiter.allow("user-exact"),
            "Must reject immediately after exactly MAX_ATTEMPTS allowed requests");
    }

    @Test
    void allow_differentIdentifiersIndependent() {
        for (int i = 0; i < 5; i++) {
            limiter.allow("user-a");
        }
        assertFalse(limiter.allow("user-a"));
        assertTrue(limiter.allow("user-b"));
    }

    @Test
    void getRemainingAttempts_firstRequest_returnsMax() {
        assertEquals(5, limiter.getRemainingAttempts("user-4"));
    }

    @Test
    void getRemainingAttempts_afterRequests_decrements() {
        limiter.allow("user-5");
        limiter.allow("user-5");
        assertEquals(3, limiter.getRemainingAttempts("user-5"));
    }

    @Test
    void getRemainingAttempts_afterExhaust_returnsZero() {
        for (int i = 0; i < 6; i++) {
            limiter.allow("user-6");
        }
        assertEquals(0, limiter.getRemainingAttempts("user-6"));
    }

    @Test
    void reset_clearsState() {
        for (int i = 0; i < 5; i++) {
            limiter.allow("user-7");
        }
        assertFalse(limiter.allow("user-7"));
        limiter.reset("user-7");
        assertTrue(limiter.allow("user-7"));
    }

    @Test
    void getResetSeconds_returnsPositive() {
        long seconds = limiter.getResetSeconds("user-8");
        assertTrue(seconds > 0 && seconds <= 60);
    }

    @Test
    void getResetSeconds_unknownIdentifier_returnsWindowSeconds() {
        assertEquals(60, limiter.getResetSeconds("unknown-user"));
    }

    @Test
    void allow_nonAsciiIdentifier_works() {
        // ConcurrentHashMap rejects null keys, so test with a valid non-ASCII key instead
        assertDoesNotThrow(() -> limiter.allow("用户-1"));
        assertTrue(limiter.allow("用户-1"));
    }
}
