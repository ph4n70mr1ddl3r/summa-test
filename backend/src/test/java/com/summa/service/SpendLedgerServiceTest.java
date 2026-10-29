package com.summa.service;

import com.summa.repository.SpendLedgerRepository;
import com.summa.model.SpendLedger;
import com.summa.model.Run;
import com.summa.model.SpawnRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.summa.exception.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class SpendLedgerServiceTest {

    @Mock
    private SpendLedgerRepository repository;

    @Mock
    private AuditService auditService;

    @Mock
    private GovernanceService governanceService;

    @InjectMocks
    private SpendLedgerService ledgerService;

    @Test
    void acknowledge_findsAndAcknowledges() {
        SpendLedger ledger = new SpendLedger();
        ledger.setId("sl-1");
        ledger.setAcknowledged(false);
        when(repository.findById("sl-1")).thenReturn(Optional.of(ledger));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SpendLedger result = ledgerService.acknowledge("sl-1", "admin");

        assertTrue(result.getAcknowledged());
        verify(auditService).log(eq("admin"), eq("ACKNOWLEDGE_OVERRUN"), eq("spend_ledger"), eq("sl-1"), isNull());
    }

    @Test
    void acknowledge_throwsWhenNotFound() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            ledgerService.acknowledge("missing", "admin"));
    }

    @Test
    void findById_returnsPresent() {
        SpendLedger ledger = new SpendLedger();
        ledger.setId("sl-1");
        when(repository.findById("sl-1")).thenReturn(Optional.of(ledger));

        Optional<SpendLedger> result = ledgerService.findById("sl-1");

        assertTrue(result.isPresent());
        assertEquals("sl-1", result.get().getId());
    }

    @Test
    void findById_returnsEmpty() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        Optional<SpendLedger> result = ledgerService.findById("missing");

        assertTrue(result.isEmpty());
    }

    @Test
    void reserveForSpawn_createsReserveWhenUnderCeiling() {
        SpawnRequest sr = new SpawnRequest();
        sr.setId("spawn-1");
        sr.setBudgetCap(25.0);

        when(governanceService.resolveSpendCeiling()).thenReturn(100.0);
        when(governanceService.resolveSpendWindowDays()).thenReturn(30L);
        when(repository.sumReservedSince(any())).thenReturn(10.0);
        when(repository.sumSettleCostSince(any())).thenReturn(20.0);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SpendLedger result = ledgerService.reserveForSpawn(sr, 25.0, "actor-1");

        assertNotNull(result);
        assertEquals("reserve", result.getKind());
        assertEquals("spawn-1", result.getSpawnId());
        assertEquals(25.0, result.getCost());
        verify(auditService).log(eq("actor-1"), eq("RESERVE_SPEND"), eq("spend_ledger"), anyString(), anyString());
    }

    @Test
    void reserveForSpawn_throwsWhenExceedingCeiling() {
        SpawnRequest sr = new SpawnRequest();
        sr.setId("spawn-1");
        sr.setBudgetCap(80.0);

        when(governanceService.resolveSpendCeiling()).thenReturn(100.0);
        when(governanceService.resolveSpendWindowDays()).thenReturn(30L);
        when(repository.sumReservedSince(any())).thenReturn(50.0);
        when(repository.sumSettleCostSince(any())).thenReturn(40.0);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
            ledgerService.reserveForSpawn(sr, 80.0, "actor-1"));
        assertTrue(ex.getMessage().contains("Spend ceiling exceeded"));
    }

    @Test
    void releaseSpawn_createsReleaseWhenNetReserveExists() {
        SpawnRequest sr = new SpawnRequest();
        sr.setId("spawn-1");

        when(repository.sumNetReserveForSpawn("spawn-1")).thenReturn(30.0);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SpendLedger result = ledgerService.releaseSpawn(sr, "actor-1");

        assertNotNull(result);
        assertEquals("release", result.getKind());
        assertEquals(30.0, result.getCost());
        verify(auditService).log(eq("actor-1"), eq("RELEASE_SPEND"), eq("spend_ledger"), anyString(), anyString());
    }

    @Test
    void reserveForRun_and_settleRun_lifecycle() {
        Run run = new Run();
        run.setId("run-1");
        run.setAgentId("agent-1");

        when(governanceService.resolveSpendCeiling()).thenReturn(50.0);
        when(governanceService.resolveSpendWindowDays()).thenReturn(30L);
        when(repository.sumReservedSince(any())).thenReturn(0.0);
        when(repository.sumSettleCostSince(any())).thenReturn(0.0);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SpendLedger reserve = ledgerService.reserveForRun(run, 15.0, "actor-1");
        assertEquals("reserve", reserve.getKind());
        assertEquals("run-1", reserve.getRunId());

        when(repository.sumNetReserveForRun("run-1")).thenReturn(15.0);
        SpendLedger settle = ledgerService.settleRun(run, 12.0, 1000L, "actor-1");
        assertEquals("settle", settle.getKind());
        assertEquals(12.0, settle.getCost());
        assertEquals(1000L, settle.getTokensOut());
        assertTrue(settle.getAcknowledged(), "Run within budget is acknowledged at settle");
    }

    @Test
    void settleRun_withOvershoot_marksOverrunUnacknowledged() {
        Run run = new Run();
        run.setId("run-1");
        run.setAgentId("agent-1");

        when(repository.sumNetReserveForRun("run-1")).thenReturn(10.0);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SpendLedger settle = ledgerService.settleRun(run, 15.0, 2000L, "actor-1");
        assertNotNull(settle);
        assertEquals("settle", settle.getKind());
        assertEquals(15.0, settle.getCost());
        assertFalse(settle.getAcknowledged(), "Overrun settle must remain unacknowledged until admin acks");
        verify(auditService).log(eq("actor-1"), eq("SPEND_OVERRUN"), eq("spend_ledger"), anyString(), anyString());
    }

    @Test
    void settleRun_withoutReserve_isAcknowledgedNotOverrun() {
        // No reserve was ever made for this run (reserveForRun is not called in the
        // run lifecycle), so a positive settle cost must not be flagged as an
        // overrun — there is no reserve to exceed.
        Run run = new Run();
        run.setId("run-2");
        run.setAgentId("agent-1");

        when(repository.sumNetReserveForRun("run-2")).thenReturn(0.0);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SpendLedger settle = ledgerService.settleRun(run, 12.0, 1000L, "actor-1");
        assertNotNull(settle);
        assertEquals("settle", settle.getKind());
        assertEquals(12.0, settle.getCost());
        assertTrue(settle.getAcknowledged(), "Settle with no reserve must be acknowledged, not an overrun");
        verify(auditService, never()).log(eq("actor-1"), eq("SPEND_OVERRUN"), anyString(), anyString(), anyString());
        verify(auditService).log(eq("actor-1"), eq("SETTLE_SPEND"), eq("spend_ledger"), anyString(), anyString());
    }

    @Test
    void checkCeilingForReserve_unacknowledgedOverrun_blocksReserves() {
        SpawnRequest sr = new SpawnRequest();
        sr.setId("spawn-1");

        when(repository.countUnacknowledgedOverruns()).thenReturn(1L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
            ledgerService.reserveForSpawn(sr, 10.0, "actor-1"));
        assertTrue(ex.getMessage().contains("Unacknowledged spend overrun exists"));
    }

    @Test
    void releaseRun_createsRelease() {
        Run run = new Run();
        run.setId("run-2");

        when(repository.sumNetReserveForRun("run-2")).thenReturn(20.0);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SpendLedger release = ledgerService.releaseRun(run, "actor-1");
        assertNotNull(release);
        assertEquals("release", release.getKind());
        assertEquals(20.0, release.getCost());
    }

    @Test
    void checkCeilingForReserve_honorsConfiguredWindowAndTotalSettled() {
        SpawnRequest sr = new SpawnRequest();
        sr.setId("spawn-1");

        when(governanceService.resolveSpendCeiling()).thenReturn(100.0);
        when(governanceService.resolveSpendWindowDays()).thenReturn(14L);
        when(repository.countUnacknowledgedOverruns()).thenReturn(0L);
        when(repository.sumReservedSince(any())).thenReturn(20.0);
        when(repository.sumSettleCostSince(any())).thenReturn(75.0);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
            ledgerService.reserveForSpawn(sr, 10.0, "actor-1"));
        assertTrue(ex.getMessage().contains("Spend ceiling exceeded"));
        verify(governanceService).resolveSpendWindowDays();
    }
}
