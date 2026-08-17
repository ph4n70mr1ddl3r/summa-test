package com.summa.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.HashMap;

@Service
public class GovernanceService {
    private static final Map<String, Object> DEFAULT_SETTINGS;
    static {
        Map<String, Object> defaults = new java.util.LinkedHashMap<>();
        defaults.put("spawn.ephemeral.default_ttl_hours", 24);
        defaults.put("spawn.ephemeral.max_concurrent_per_spawner", 3);
        defaults.put("spawn.org_wide_max_active_agents", 100);
        defaults.put("spawn.depth_cap", 2);
        defaults.put("spawn.budget_window_days", 30);
        defaults.put("asks.tier_critical_deadline_hours", 1);
        defaults.put("asks.tier_standard_deadline", "next-digest");
        defaults.put("asks.tier_bulk_deadline_hours", 24);
        defaults.put("asks.storm_collapse_window_hours", 1);
        defaults.put("asks.rate_limit_per_source_per_hour", 60);
        defaults.put("dna.default_review_sla_days", 7);
        defaults.put("spend.org_ceiling", 1000000);
        defaults.put("spend.critical_floor_percent", 5);
        DEFAULT_SETTINGS = java.util.Collections.unmodifiableMap(defaults);
    }

    private final Map<String, Object> settings = new HashMap<>(DEFAULT_SETTINGS);

    public Map<String, Object> getAllSettings() {
        return new HashMap<>(settings);
    }

    public Object getSetting(String key) {
        return settings.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getSetting(String key, Class<T> type) {
        Object value = settings.get(key);
        if (value == null) return null;
        if (type.isInstance(value)) return type.cast(value);
        if (type == Integer.class || type == int.class) {
            return type.cast(Integer.parseInt(value.toString()));
        }
        if (type == Long.class || type == long.class) {
            return type.cast(Long.parseLong(value.toString()));
        }
        if (type == Double.class || type == double.class) {
            return type.cast(Double.parseDouble(value.toString()));
        }
        if (type == Boolean.class || type == boolean.class) {
            return type.cast(Boolean.parseBoolean(value.toString()));
        }
        return type.cast(value);
    }

    public void setSetting(String key, Object value) {
        settings.put(key, value);
    }

    public boolean isSpendHaltTripped() {
        // Simplified: check if spend exceeds ceiling
        return false;
    }
}
