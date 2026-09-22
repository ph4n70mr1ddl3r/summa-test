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
}
