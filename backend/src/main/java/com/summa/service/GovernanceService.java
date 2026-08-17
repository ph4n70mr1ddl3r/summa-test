package com.summa.service;

import com.summa.repository.SpendLedgerRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

@Service
public class GovernanceService {
    private final SpendLedgerRepository spendLedgerRepository;
    private final Map<String, Object> settings = new HashMap<>();

    public GovernanceService(SpendLedgerRepository spendLedgerRepository) {
        this.spendLedgerRepository = spendLedgerRepository;
        // Defaults per CFG-040..024
        settings.put("spawn.ephemeral.default_ttl_hours", 24);
        settings.put("spawn.ephemeral.max_concurrent_per_spawner", 3);
        settings.put("spawn.org_wide_max_active_agents", 100);
        settings.put("spawn.depth_cap", 2);
        settings.put("spawn.budget_window_days", 30);
        settings.put("asks.tier_critical_deadline_hours", 1);
        settings.put("asks.tier_standard_deadline", "next-digest");
        settings.put("asks.tier_bulk_deadline_hours", 24);
        settings.put("asks.storm_collapse_window_hours", 1);
        settings.put("asks.rate_limit_per_source_per_hour", 60);
        settings.put("dna.default_review_sla_days", 7);
        settings.put("spend.org_ceiling", 1000000.0);
        settings.put("spend.critical_floor_percent", 5.0);
    }

    public Map<String, Object> getAllSettings() {
        return new HashMap<>(settings);
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

    public Object getSetting(String key) {
        return settings.get(key);
    }

    public void setSetting(String key, Object value) {
        settings.put(key, value);
    }

    /**
     * SPW-060: Check if the spend circuit-breaker should trip.
     * Compares total spend (last 30 days) against the org ceiling.
     */
    public boolean isSpendHaltTripped() {
        Double totalCost = spendLedgerRepository.sumTotalCostSince(
            Instant.now().minusSeconds(30 * 86400L));
        if (totalCost == null) totalCost = 0.0;
        
        Double ceiling = getSetting("spend.org_ceiling", Double.class);
        if (ceiling == null) ceiling = 1000000.0;
        
        return totalCost >= ceiling;
    }

    public Map<String, Object> getSpendView() {
        Double totalCost = spendLedgerRepository.sumTotalCostSince(
            Instant.now().minusSeconds(30 * 86400L));
        if (totalCost == null) totalCost = 0.0;
        
        Double ceiling = getSetting("spend.org_ceiling", Double.class);
        if (ceiling == null) ceiling = 1000000.0;
        
        java.util.Map<String, Object> view = new java.util.LinkedHashMap<>();
        view.put("totalCost30d", totalCost);
        view.put("ceiling", ceiling);
        view.put("utilization", totalCost / ceiling);
        view.put("halted", isSpendHaltTripped());
        return view;
    }
}
