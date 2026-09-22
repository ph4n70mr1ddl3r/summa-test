package com.summa.constants;

/**
 * Shared constant definitions to prevent divergence between services.
 */
public final class Defaults {
    private Defaults() {}

    public static final double DEFAULT_SPEND_CEILING = 1_000_000.0;
    public static final long DEFAULT_EVALUATION_WINDOW_DAYS = 30;
    public static final long DEFAULT_CRITICAL_ASK_DEADLINE_HOURS = 1;
    public static final long DEFAULT_BULK_ASK_DEADLINE_HOURS = 24;
    public static final long DEFAULT_STANDARD_ASK_DEADLINE_HOURS = 24;
    public static final int DEFAULT_SPAWN_EPHEMERAL_DEFAULT_TTL_HOURS = 24;
    public static final int DEFAULT_SPAWN_EPHEMERAL_MAX_CONCURRENT_PER_SPAWNER = 3;
    public static final int DEFAULT_SPAWN_ORG_WIDE_MAX_ACTIVE_AGENTS = 100;
    public static final int DEFAULT_SPAWN_DEPTH_CAP = 2;
    public static final int DEFAULT_ASKS_STORM_COLLAPSE_WINDOW_HOURS = 1;
    public static final int DEFAULT_ASKS_RATE_LIMIT_PER_SOURCE_PER_HOUR = 60;
    public static final int DEFAULT_DNA_DEFAULT_REVIEW_SLA_DAYS = 7;
    public static final double DEFAULT_SPEND_CRITICAL_FLOOR_PERCENT = 5.0;
    public static final String SYSTEM_ACTOR = "system";

    // Scheduling intervals (milliseconds)
    public static final long STALL_CHECK_INTERVAL_MS = 300000L;
    public static final long TTL_REAP_INTERVAL_MS = 300000L;

    // Timeouts (seconds)
    public static final long STALL_ASK_DEADLINE_SECONDS = 7L * 86400;
    public static final long STALL_ASK_DEDUP_WINDOW_SECONDS = 3600L;
    public static final long ENROLLMENT_TOKEN_TTL_SECONDS = 3600L;
    public static final long REBIND_ASK_DEADLINE_SECONDS = 7L * 86400;
    public static final long MAX_DEADLINE_SECONDS = 365L * 86400;

    // Algorithmic bounds
    public static final int MAX_EXPIRE_SUCCESSOR_DEPTH = 5;
    public static final int DEFAULT_CYCLE_DETECTION_MAX_STEPS = 50;
}
