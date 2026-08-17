package com.summa.service;

import com.summa.repository.GovernanceSettingRepository;
import com.summa.model.GovernanceSetting;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
public class GovernanceService {
    private final GovernanceSettingRepository settingRepository;

    public GovernanceService(GovernanceSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public Map<String, Object> getAllSettings() {
        Map<String, Object> settings = new HashMap<>();
        for (GovernanceSetting s : settingRepository.findAll()) {
            settings.put(s.getKey(), parseValue(s.getValue()));
        }
        // Apply defaults for any missing keys
        applyDefaults(settings);
        return settings;
    }

    @SuppressWarnings("unchecked")
    public <T> T getSetting(String key, Class<T> type) {
        Map<String, Object> settings = getAllSettings();
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
        Map<String, Object> settings = getAllSettings();
        return settings.get(key);
    }

    public void setSetting(String key, Object value, String editedBy) {
        String serialized = serializeValue(value);
        Optional<GovernanceSetting> existing = settingRepository.findById(key);
        GovernanceSetting setting;
        if (existing.isPresent()) {
            setting = existing.get();
            setting.setValue(serialized);
        } else {
            setting = new GovernanceSetting();
            setting.setKey(key);
            setting.setValue(serialized);
        }
        setting.setEditedBy(editedBy);
        setting.setEditedAt(Instant.now());
        settingRepository.save(setting);
    }

    /**
     * SPW-060: Check if the spend circuit-breaker should trip.
     * Compares total spend (last 30 days) against the org ceiling.
     */
    public boolean isSpendHaltTripped() {
        // Stub: delegate to SpendLedgerRepository when available
        return false;
    }

    public Map<String, Object> getSpendView() {
        Double ceiling = getSetting("spend-org-ceiling", Double.class);
        if (ceiling == null) ceiling = 1000000.0;
        java.util.Map<String, Object> view = new java.util.LinkedHashMap<>();
        view.put("totalCost30d", 0.0);
        view.put("ceiling", ceiling);
        view.put("utilization", 0.0);
        view.put("halted", false);
        return view;
    }

    private void applyDefaults(Map<String, Object> settings) {
        settings.putIfAbsent("spawn-ephemeral-default-ttl-hours", 24);
        settings.putIfAbsent("spawn-ephemeral-max-concurrent-per-spawner", 3);
        settings.putIfAbsent("spawn-org-wide-max-active-agents", 100);
        settings.putIfAbsent("spawn-depth-cap", 2);
        settings.putIfAbsent("spawn-budget-window-days", 30);
        settings.putIfAbsent("asks-tier-critical-deadline-hours", 1);
        settings.putIfAbsent("asks-tier-standard-deadline", "next-digest");
        settings.putIfAbsent("asks-tier-bulk-deadline-hours", 24);
        settings.putIfAbsent("asks-storm-collapse-window-hours", 1);
        settings.putIfAbsent("asks-rate-limit-per-source-per-hour", 60);
        settings.putIfAbsent("dna-default-review-sla-days", 7);
        settings.putIfAbsent("spend-org-ceiling", 1000000.0);
        settings.putIfAbsent("spend-critical-floor-percent", 5.0);
    }

    private Object parseValue(String value) {
        if (value == null) return "{}";
        try {
            if (value.matches("-?\\d+(\\.\\d+)?")) {
                if (value.contains(".")) return Double.parseDouble(value);
                return Long.parseLong(value);
            }
            if ("true".equalsIgnoreCase(value)) return true;
            if ("false".equalsIgnoreCase(value)) return false;
        } catch (NumberFormatException e) {
            // not a number
        }
        return value;
    }

    private String serializeValue(Object value) {
        if (value == null) return "{}";
        return value.toString();
    }
}
