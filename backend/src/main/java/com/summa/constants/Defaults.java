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
}
