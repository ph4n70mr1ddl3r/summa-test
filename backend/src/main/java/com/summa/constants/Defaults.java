package com.summa.constants;

import java.util.Arrays;
import java.util.Set;

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
    public static final long DEFAULT_SPAWN_BUDGET_WINDOW_DAYS = 30;
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

    // List limits
    public static final int DEFAULT_LIST_LIMIT = 50;
    public static final int MAX_LIST_LIMIT = 200;
    public static final int MAX_AUDIT_LOG_LIMIT = 1000;
    public static final int MAX_DNA_SEARCH_LIMIT = 100;
    public static final int MAX_ASK_EXPIRY_DAYS = 365;

    // Governance key sets — must stay in sync with GovernanceController allowed-key checks
    public static final Set<String> POLICY_KEYS = new java.util.LinkedHashSet<>(java.util.Arrays.asList(
            "asks-tier-critical-deadline-hours",
            "asks-tier-standard-deadline-hours",
            "asks-tier-bulk-deadline-hours",
            "asks-storm-collapse-window-hours",
            "asks-rate-limit-per-source-per-hour",
            "summa.dna.default-review-sla-days",
            "spend-org-ceiling",
            "spend-critical-floor-percent",
            "spend-evaluation-window-days"
    ));

    public static final Set<String> QUOTA_KEYS = new java.util.LinkedHashSet<>(java.util.Arrays.asList(
            "spawn-ephemeral-default-ttl-hours",
            "spawn-ephemeral-max-concurrent-per-spawner",
            "spawn-org-wide-max-active-agents",
            "spawn-depth-cap",
            "spawn-budget-window-days",
            "node-affinity-starvation-hours"
    ));

    // PAT limits
    public static final int MAX_PAT_EXPIRY_DAYS = 365;

    // Node affinity
    public static final long DEFAULT_NODE_AFFINITY_STARVATION_HOURS = 24;

    // Node pubkey validation — must match NodeAuthFilter computeSignature logic
    public static final String PUBKEY_REGEX =
        "^[A-Za-z0-9+/]{44}={1,3}$" +
        "|^[A-Za-z0-9_-]{43}[=_]{1}$" +
        "|^[A-Za-z0-9+/]{88}={1,3}$" +
        "|^[A-Za-z0-9_-]{64}$" +
        "|^[A-Za-z0-9_-]{86}[=_]{1}$";
}
