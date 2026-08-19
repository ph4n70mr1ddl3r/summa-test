package com.summa.service;

import com.summa.repository.GovernanceSettingRepository;
import com.summa.repository.SpendLedgerRepository;
import com.summa.model.GovernanceSetting;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;

@Service
public class GovernanceService {
    private final GovernanceSettingRepository settingRepository;
    private final SpendLedgerRepository spendLedgerRepository;

    private static final double DEFAULT_SPEND_CEILING = 1_000_000.0;
    private static final long DEFAULT_EVALUATION_WINDOW_DAYS = 30;
    private static final long DEFAULT_CRITICAL_ASK_DEADLINE_HOURS = 1;
    private static final long DEFAULT_BULK_ASK_DEADLINE_HOURS = 24;
    private static final long DEFAULT_STANDARD_ASK_DEADLINE_HOURS = 24;

    public GovernanceService(GovernanceSettingRepository settingRepository,
                              SpendLedgerRepository spendLedgerRepository) {
        this.settingRepository = settingRepository;
        this.spendLedgerRepository = spendLedgerRepository;
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
        throw new IllegalArgumentException("Cannot cast value to " + type.getName() + " for key: " + key);
    }

    public Object getSetting(String key) {
        Map<String, Object> settings = getAllSettings();
        return settings.get(key);
    }

    @Transactional
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
     * Compares (reserved + settled) against the org ceiling per SPW-033.
     * The evaluation window is configurable via governance setting
     * "spend-evaluation-window-days" (default 30).
     */
    public boolean isSpendHaltTripped() {
        try {
            Double ceiling = getSetting("spend-org-ceiling", Double.class);
            if (ceiling == null) ceiling = DEFAULT_SPEND_CEILING;
            Object windowDaysObj = getSetting("spend-evaluation-window-days");
            long windowDays = DEFAULT_EVALUATION_WINDOW_DAYS;
            if (windowDaysObj instanceof Number) {
                windowDays = ((Number) windowDaysObj).longValue();
            }
            Instant window = Instant.now().minus(windowDays, ChronoUnit.DAYS);
            Double reservedObj = spendLedgerRepository.sumReservedSince(window);
            Double settledObj = spendLedgerRepository.sumSettleCostSince(window);
            double reserved = reservedObj != null ? reservedObj : 0.0;
            double settled = settledObj != null ? settledObj : 0.0;
            return (reserved + settled) >= ceiling;
        } catch (Exception e) {
            // Fail-closed: any error in spend calculation trips the breaker
            return true;
        }
    }

    public Map<String, Object> getSpendView() {
        Double ceiling = getSetting("spend-org-ceiling", Double.class);
        if (ceiling == null) ceiling = DEFAULT_SPEND_CEILING;
        Object windowDaysObj = getSetting("spend-evaluation-window-days");
        long windowDays = DEFAULT_EVALUATION_WINDOW_DAYS;
        if (windowDaysObj instanceof Number) {
            windowDays = ((Number) windowDaysObj).longValue();
        }
        Instant window = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        Double reservedObj = spendLedgerRepository.sumReservedSince(window);
        Double settledObj = spendLedgerRepository.sumSettleCostSince(window);
        double reserved = reservedObj != null ? reservedObj : 0.0;
        double settled = settledObj != null ? settledObj : 0.0;
        double utilization = (reserved + settled) / ceiling;
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("reserved", reserved);
        view.put("settled", settled);
        view.put("ceiling", ceiling);
        view.put("utilization", String.format("%.2f%%", utilization * 100));
        view.put("halted", isSpendHaltTripped());
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
        settings.putIfAbsent("spend-org-ceiling", DEFAULT_SPEND_CEILING);
        settings.putIfAbsent("spend-critical-floor-percent", 5.0);
        settings.putIfAbsent("spend-evaluation-window-days", DEFAULT_EVALUATION_WINDOW_DAYS);
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
