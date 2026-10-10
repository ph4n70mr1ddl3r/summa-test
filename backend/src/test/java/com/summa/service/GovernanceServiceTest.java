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
        when(spendLedgerRepository.sumUnacknowledgedReservedSince(any())).thenReturn(1500000.0);
        when(spendLedgerRepository.sumUnacknowledgedSettleCostSince(any())).thenReturn(0.0);

        assertTrue(governanceService.isSpendHaltTripped());
    }

    @Test
    void isSpendHaltTripped_returnsFalseWhenCostBelowCeiling() {
        GovernanceSetting setting = new GovernanceSetting();
        setting.setKey("spend-org-ceiling");
        setting.setValue("1000000");
        when(settingRepository.findAll()).thenReturn(java.util.List.of(setting));
        when(spendLedgerRepository.sumUnacknowledgedReservedSince(any())).thenReturn(200000.0);
        when(spendLedgerRepository.sumUnacknowledgedSettleCostSince(any())).thenReturn(100000.0);

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
}
