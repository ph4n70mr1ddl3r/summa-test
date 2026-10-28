package com.summa.service;

import com.summa.repository.SpendLedgerRepository;
import com.summa.model.SpendLedger;
import com.summa.model.Run;
import com.summa.model.SpawnRequest;
import com.summa.constants.Defaults;
import com.summa.util.JsonHelpers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import com.summa.exception.EntityNotFoundException;

@Service
public class SpendLedgerService {
    private final SpendLedgerRepository repository;
    private final AuditService auditService;
    private final GovernanceService governanceService;

    public SpendLedgerService(SpendLedgerRepository repository, AuditService auditService) {
        this(repository, auditService, null);
    }

    public SpendLedgerService(SpendLedgerRepository repository, AuditService auditService,
                              GovernanceService governanceService) {
        this.repository = repository;
        this.auditService = auditService;
        this.governanceService = governanceService;
    }

    @Transactional(readOnly = true)
    public Optional<SpendLedger> findById(String id) {
        return repository.findById(id);
    }

    @Transactional
    public SpendLedger acknowledge(String id, String actor) {
        SpendLedger ledger = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Spend ledger row not found: " + id));
        // Idempotency: a duplicate ack (e.g. controller retry) must not emit
        // a second audit row for the same logical act.
        if (Boolean.TRUE.equals(ledger.getAcknowledged())) {
            return ledger;
        }
        ledger.setAcknowledged(true);
        SpendLedger saved = repository.save(ledger);
        auditService.log(actor, "ACKNOWLEDGE_OVERRUN", "spend_ledger", id, null);
        return saved;
    }

    /**
     * SPW-033 / SPW-034: Reserve budget atomically against (reserved + settled)
     * in the spend ledger for a spawn request.
     */
    @Transactional
    public SpendLedger reserveForSpawn(SpawnRequest request, Double amount, String actor) {
        if (amount == null || amount <= 0 || request == null) {
            return null;
        }
        checkCeilingForReserve(amount);
        String memberId = request.getRequesterId() != null ? request.getRequesterId() : Defaults.SYSTEM_ACTOR;
        SpendLedger ledger = new SpendLedger();
        ledger.setId(UUID.randomUUID().toString());
        ledger.setMemberId(memberId);
        ledger.setSpawnId(request.getId());
        ledger.setKind("reserve");
        ledger.setCost(amount);
        ledger.setTokensIn(0.0);
        ledger.setTokensOut(0L);
        ledger.setAcknowledged(false);
        SpendLedger saved = repository.save(ledger);
        auditService.log(actor, "RESERVE_SPEND", "spend_ledger", saved.getId(),
            String.format("{\"spawnId\":%s,\"cost\":%.4f}", JsonHelpers.jsonString(request.getId()), amount));
        return saved;
    }

    /**
     * SPW-033 / SPW-034: Release reserved budget when a spawn request terminates
     * (denial, expiry, retraction, halt archive).
     */
    @Transactional
    public SpendLedger releaseSpawn(SpawnRequest request, String actor) {
        if (request == null || request.getId() == null) return null;
        double netReserve = repository.sumNetReserveForSpawn(request.getId());
        if (netReserve <= 0) return null;

        String memberId = request.getRequesterId() != null ? request.getRequesterId() : Defaults.SYSTEM_ACTOR;
        SpendLedger ledger = new SpendLedger();
        ledger.setId(UUID.randomUUID().toString());
        ledger.setMemberId(memberId);
        ledger.setSpawnId(request.getId());
        ledger.setKind("release");
        ledger.setCost(netReserve);
        ledger.setTokensIn(0.0);
        ledger.setTokensOut(0L);
        ledger.setAcknowledged(false);
        SpendLedger saved = repository.save(ledger);
        auditService.log(actor, "RELEASE_SPEND", "spend_ledger", saved.getId(),
            String.format("{\"spawnId\":%s,\"cost\":%.4f}", JsonHelpers.jsonString(request.getId()), netReserve));
        return saved;
    }

    /**
     * SPW-033: Reserve budget atomically against (reserved + settled) for a run.
     */
    @Transactional
    public SpendLedger reserveForRun(Run run, Double amount, String actor) {
        if (amount == null || amount <= 0 || run == null) {
            return null;
        }
        checkCeilingForReserve(amount);
        String memberId = run.getAgentId() != null ? run.getAgentId() : Defaults.SYSTEM_ACTOR;
        SpendLedger ledger = new SpendLedger();
        ledger.setId(UUID.randomUUID().toString());
        ledger.setMemberId(memberId);
        ledger.setRunId(run.getId());
        ledger.setKind("reserve");
        ledger.setCost(amount);
        ledger.setTokensIn(0.0);
        ledger.setTokensOut(0L);
        ledger.setAcknowledged(false);
        SpendLedger saved = repository.save(ledger);
        auditService.log(actor, "RESERVE_SPEND", "spend_ledger", saved.getId(),
            String.format("{\"runId\":%s,\"cost\":%.4f}", JsonHelpers.jsonString(run.getId()), amount));
        return saved;
    }

    /**
     * SPW-033: Release reserved budget for a run upon failure, cancellation, or reaping.
     */
    @Transactional
    public SpendLedger releaseRun(Run run, String actor) {
        if (run == null || run.getId() == null) return null;
        double netReserve = repository.sumNetReserveForRun(run.getId());
        if (netReserve <= 0) return null;

        String memberId = run.getAgentId() != null ? run.getAgentId() : Defaults.SYSTEM_ACTOR;
        SpendLedger ledger = new SpendLedger();
        ledger.setId(UUID.randomUUID().toString());
        ledger.setMemberId(memberId);
        ledger.setRunId(run.getId());
        ledger.setKind("release");
        ledger.setCost(netReserve);
        ledger.setTokensIn(0.0);
        ledger.setTokensOut(0L);
        ledger.setAcknowledged(false);
        SpendLedger saved = repository.save(ledger);
        auditService.log(actor, "RELEASE_SPEND", "spend_ledger", saved.getId(),
            String.format("{\"runId\":%s,\"cost\":%.4f}", JsonHelpers.jsonString(run.getId()), netReserve));
        return saved;
    }

    /**
     * SPW-033 / SPW-035: Settle actual run cost at completion. Releases any active
     * reserve for the run and appends the settle row.
     */
    @Transactional
    public SpendLedger settleRun(Run run, Double actualCost, Long tokensOut, String actor) {
        if (run == null || run.getId() == null) return null;
        releaseRun(run, actor);

        if (actualCost != null && actualCost > 0 && run.getAgentId() != null) {
            SpendLedger ledger = new SpendLedger();
            ledger.setId(UUID.randomUUID().toString());
            ledger.setMemberId(run.getAgentId());
            ledger.setRunId(run.getId());
            ledger.setKind("settle");
            ledger.setCost(actualCost);
            ledger.setTokensOut(tokensOut != null ? tokensOut : 0L);
            ledger.setAcknowledged(false);
            SpendLedger saved = repository.save(ledger);
            auditService.log(actor, "SETTLE_SPEND", "spend_ledger", saved.getId(),
                String.format("{\"runId\":%s,\"cost\":%.4f}", JsonHelpers.jsonString(run.getId()), actualCost));
            return saved;
        }
        return null;
    }

    private void checkCeilingForReserve(double amount) {
        if (governanceService == null) return;
        if (governanceService.isSpendHaltTripped()) {
            throw new IllegalStateException("Spend halt is active — spend reservations are blocked");
        }
        double ceiling = governanceService.resolveSpendCeiling();
        Instant window = Instant.now().minus(Defaults.DEFAULT_EVALUATION_WINDOW_DAYS, ChronoUnit.DAYS);
        double reserved = repository.sumUnacknowledgedReservedSince(window);
        double settled = repository.sumUnacknowledgedSettleCostSince(window);
        if ((reserved + settled + amount) > ceiling) {
            throw new IllegalStateException(
                String.format("Spend ceiling exceeded: cannot reserve %.4f against ceiling %.4f (current reserved+settled: %.4f)",
                    amount, ceiling, (reserved + settled)));
        }
    }
}
