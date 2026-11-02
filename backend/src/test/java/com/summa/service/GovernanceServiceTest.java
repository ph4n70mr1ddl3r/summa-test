package com.summa.service;

import com.summa.repository.GovernanceSettingRepository;
import com.summa.repository.SpendLedgerRepository;
import com.summa.model.GovernanceSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GovernanceServiceTest {

    @Mock
    private GovernanceSettingRepository settingRepository;

    @Mock
    private SpendLedgerRepository spendLedgerRepository;

    @InjectMocks
    private GovernanceService governanceService;

    @Test
    void isSpendHaltTripped_returnsTrueWhenCostExceedsCeiling() {
        when(spendLedgerRepository.sumReservedSince(any())).thenReturn(1500000.0);
        when(spendLedgerRepository.sumSettleCostSince(any())).thenReturn(0.0);

        assertTrue(governanceService.isSpendHaltTripped());
    }

    @Test
    void isSpendHaltTripped_returnsFalseWhenCostBelowCeiling() {
        GovernanceSetting setting = new GovernanceSetting();
        setting.setKey("spend-org-ceiling");
        setting.setValue("1000000");
        when(settingRepository.findAll()).thenReturn(java.util.List.of(setting));
        when(spendLedgerRepository.sumReservedSince(any())).thenReturn(200000.0);
        when(spendLedgerRepository.sumSettleCostSince(any())).thenReturn(100000.0);

        assertFalse(governanceService.isSpendHaltTripped());
    }

    @Test
    void isSpendHaltTripped_failsClosedOnError() {
        // Simulate exception in query — should fail-closed per SEC-011/SPW-060;
        // a DB error must trip the breaker rather than silently allow spending.
        when(settingRepository.findAll()).thenThrow(new RuntimeException("DB error"));

        assertTrue(governanceService.isSpendHaltTripped());
    }

    @Test
    void getSetting_parsesLongSetting() {
        GovernanceSetting setting = new GovernanceSetting();
        setting.setKey("spawn-depth-cap");
        setting.setValue("2");
        when(settingRepository.findAll()).thenReturn(java.util.List.of(setting));

        Long result = governanceService.getSetting("spawn-depth-cap", Long.class);
        assertEquals(2L, result);
    }

    @Test
    void applyDefaults_preservesExplicitSettings() {
        GovernanceSetting setting = new GovernanceSetting();
        setting.setKey("spawn-ephemeral-default-ttl-hours");
        setting.setValue("48");
        when(settingRepository.findAll()).thenReturn(java.util.List.of(setting));

        var settings = governanceService.getAllSettings();
        assertEquals(48L, settings.get("spawn-ephemeral-default-ttl-hours"));
    }

    @Test
    void applyDefaults_includesAllDefaultValues() {
        when(settingRepository.findAll()).thenReturn(java.util.List.of());

        var settings = governanceService.getAllSettings();
        assertEquals(24, ((Number) settings.get("spawn-ephemeral-default-ttl-hours")).intValue());
        assertEquals(3, ((Number) settings.get("spawn-ephemeral-max-concurrent-per-spawner")).intValue());
        assertEquals(100, ((Number) settings.get("spawn-org-wide-max-active-agents")).intValue());
        assertEquals(2, ((Number) settings.get("spawn-depth-cap")).intValue());
        assertEquals(30, ((Number) settings.get("spawn-budget-window-days")).intValue());
        assertEquals(1, ((Number) settings.get("asks-tier-critical-deadline-hours")).intValue());
        assertEquals(24, ((Number) settings.get("asks-tier-standard-deadline-hours")).intValue());
        assertEquals(24, ((Number) settings.get("asks-tier-bulk-deadline-hours")).intValue());
        assertEquals(1, ((Number) settings.get("asks-storm-collapse-window-hours")).intValue());
        assertEquals(60, ((Number) settings.get("asks-rate-limit-per-source-per-hour")).intValue());
        assertEquals(7, ((Number) settings.get("summa.dna.default-review-sla-days")).intValue());
        assertEquals(5.0, ((Number) settings.get("spend-critical-floor-percent")).doubleValue());
        assertEquals(30, ((Number) settings.get("spend-evaluation-window-days")).intValue());
        assertEquals(24, ((Number) settings.get("node-affinity-starvation-hours")).intValue());
    }

    @Test
    void setSetting_nullValue_deletesExistingSetting() {
        GovernanceSetting setting = new GovernanceSetting();
        setting.setKey("spawn-depth-cap");
        setting.setValue("99");
        when(settingRepository.existsById("spawn-depth-cap")).thenReturn(true);

        governanceService.setSetting("spawn-depth-cap", null, "admin");

        verify(settingRepository).deleteById("spawn-depth-cap");
    }

    @Test
    void setSetting_nullValue_missingKey_isNoOp() {
        // Regression: clearing a key the server never stored blew up the request
        // with an integrity error — the console's "clear value" path sends
        // {key: null} without checking what the server holds first.
        lenient().when(settingRepository.existsById("spawn-depth-cap")).thenReturn(false);

        assertDoesNotThrow(() -> governanceService.setSetting("spawn-depth-cap", null, "admin"));
        verify(settingRepository, never()).deleteById("spawn-depth-cap");
    }

    @Test
    void setSetting_nullValue_invalidatesWarmCache() {
        // Regression: a cleared key vanished from the database but the warm
        // settings cache still served its old value for up to the TTL — readers
        // (spawn gates, ask deadlines) kept using a setting the admin removed.
        GovernanceSetting setting = new GovernanceSetting();
        setting.setKey("spawn-depth-cap");
        setting.setValue("99");
        when(settingRepository.findAll()).thenReturn(java.util.List.of(setting));
        lenient().when(settingRepository.existsById("spawn-depth-cap")).thenReturn(true);

        governanceService.getAllSettings(); // warm the cache with the stored value
        governanceService.setSetting("spawn-depth-cap", null, "admin");
        when(settingRepository.findAll()).thenReturn(java.util.List.of()); // row is gone

        Map<String, Object> after = governanceService.getAllSettings();
        assertEquals(2, ((Number) after.get("spawn-depth-cap")).intValue(),
            "after a clear the key must fall back to its default, not the stale cached value");
    }

    @Test
    void getSetting_parsesFloatSetting() {
        GovernanceSetting setting = new GovernanceSetting();
        setting.setKey("spend-critical-floor-percent");
        setting.setValue("5.5");
        when(settingRepository.findAll()).thenReturn(java.util.List.of(setting));

        Float result = governanceService.getSetting("spend-critical-floor-percent", Float.class);
        assertEquals(5.5f, result, 0.001f);
    }

    @Test
    void getSetting_NumberClassReturnsNumber() {
        GovernanceSetting setting = new GovernanceSetting();
        setting.setKey("asks-tier-critical-deadline-hours");
        setting.setValue("1");
        when(settingRepository.findAll()).thenReturn(java.util.List.of(setting));

        Number result = governanceService.getSetting("asks-tier-critical-deadline-hours", Number.class);
        assertNotNull(result);
        assertEquals(1L, result.longValue());
    }

    @Test
    void getSetting_NumberClassFloatReturnsNumber() {
        GovernanceSetting setting = new GovernanceSetting();
        setting.setKey("asks-tier-critical-deadline-hours");
        setting.setValue("1.5");
        when(settingRepository.findAll()).thenReturn(java.util.List.of(setting));

        Number result = governanceService.getSetting("asks-tier-critical-deadline-hours", Number.class);
        assertNotNull(result);
        assertEquals(1.5, result.doubleValue(), 0.0001);
    }

    @Test
    void getAllSettings_warmCacheStillServesDefaults() {
        // Regression: defaults used to be baked into the cache only at fill time.
        // A caller that deleted or never set a key got the default on a cold
        // cache but null from a warm one, until the next cache miss.
        when(settingRepository.findAll()).thenReturn(java.util.List.of());

        Map<String, Object> cold = governanceService.getAllSettings();
        assertNotNull(cold.get("spawn-ephemeral-default-ttl-hours"),
            "cold cache must carry the default");

        Map<String, Object> warm = governanceService.getAllSettings();
        assertEquals(cold, warm, "warm cache must return the same value set");
        assertEquals(24, ((Number) warm.get("spawn-ephemeral-default-ttl-hours")).intValue());
        assertEquals(7, ((Number) warm.get("summa.dna.default-review-sla-days")).intValue());

        // getSetting reads through the same cache, so it must also see the
        // default while the cache is warm.
        Integer result = governanceService.getSetting("spawn-ephemeral-default-ttl-hours", Integer.class);
        assertEquals(24, result);
    }
}
