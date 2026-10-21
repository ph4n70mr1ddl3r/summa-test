package com.summa.service;

import com.summa.repository.RunRepository;
import com.summa.model.Run;
import com.summa.model.Agent;
import com.summa.model.SpendLedger;
import com.summa.repository.AgentRepository;
import com.summa.repository.SpendLedgerRepository;
import com.summa.repository.InitiativeRepository;
import com.summa.model.Initiative;
import com.summa.constants.Defaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.summa.exception.EntityNotFoundException;
import com.summa.util.JsonHelpers;

@Service
public class RunService {
    private final RunRepository runRepository;
    private final AuditService auditService;
    private final InitiativeRepository initiativeRepository;
    private final AgentRepository agentRepository;
    private final SpendLedgerRepository spendLedgerRepository;
    private final MemberService memberService;

    public RunService(RunRepository runRepository, AuditService auditService,
                       InitiativeRepository initiativeRepository, AgentRepository agentRepository,
                       SpendLedgerRepository spendLedgerRepository, MemberService memberService) {
        this.runRepository = runRepository;
        this.auditService = auditService;
        this.initiativeRepository = initiativeRepository;
        this.agentRepository = agentRepository;
        this.spendLedgerRepository = spendLedgerRepository;
        this.memberService = memberService;
    }

    @Transactional
    public Run create(String agentId, String workspaceId, String initiativeId,
                        String triggerId, String prompt, String actor) {
        // INT-080: Only active initiatives launch runs
        if (initiativeId != null && !initiativeId.isBlank()) {
            Optional<Initiative> initOpt = initiativeRepository.findById(initiativeId);
            if (initOpt.isEmpty()) {
                throw new EntityNotFoundException("Initiative not found: " + initiativeId);
            }
            Initiative init = initOpt.get();
            if (!"active".equals(init.getStatus())) {
                throw new IllegalStateException(
                    "Cannot launch run under non-active initiative: " + initiativeId
                        + " (status: " + init.getStatus() + ")");
            }
        }

        Run run = new Run();
        run.setId(UUID.randomUUID().toString());
        run.setAgentId(agentId);
        run.setWorkspaceId(workspaceId);
        run.setInitiativeId(initiativeId);
        run.setTriggerId(triggerId);
        run.setPrompt(prompt != null ? prompt : "");
        run.setStatus("queued");

        Run saved = runRepository.save(run);
        auditService.log(actor, "CREATE_RUN", "run", saved.getId(),
            String.format("{\"agentId\":%s,\"status\":%s}", JsonHelpers.jsonString(agentId), JsonHelpers.jsonString("queued")));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Run> findById(String id) {
        return runRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Run> findByAgent(String agentId, int limit) {
        return runRepository.findByAgentIdOrderByCreatedAtDesc(agentId, limit);
    }

    @Transactional(readOnly = true)
    public List<Run> findByWorkspace(String workspaceId, int limit) {
        return runRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId, limit);
    }

    @Transactional(readOnly = true)
    public List<Run> findByStatus(String status, int limit) {
        return runRepository.findByStatusOrderByCreatedAtDesc(status, limit);
    }

    @Transactional(readOnly = true)
    public List<Run> findRecent(int limit) {
        return runRepository.findByOrderByCreatedAtDesc(limit);
    }

    @Transactional
    public Run start(String id) {
        return start(id, Defaults.SYSTEM_ACTOR);
    }

    @Transactional
    public Run start(String id, String actor) {
        Run run = runRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + id));
        requireRunAuthority(run, actor, "start");
        if (!"queued".equals(run.getStatus())) {
            throw new IllegalStateException("Cannot start run with status: " + run.getStatus());
        }
        run.setStatus("running");
        run.setStartedAt(Instant.now());
        Run saved = runRepository.save(run);
        auditService.logSystem("START_RUN", "run", id, null);
        return saved;
    }

    /**
     * Run-transition authority: a run belongs to the agent that runs it, and the
     * agent belongs to its owner human. Only that agent, that owner, or an active
     * admin may drive the run's lifecycle — a plain member may not cancel,
     * complete, or fail another member's run (the write gate alone is not
     * authority). The system's own walks act as the system.
     */
    private void requireRunAuthority(Run run, String actor, String action) {
        if (actor == null || actor.isBlank() || Defaults.SYSTEM_ACTOR.equals(actor)) return;
        String clean = JsonHelpers.stripIdPrefix(actor);
        if (clean == null || clean.isBlank()) return;
        if (memberService.isAdmin(clean)) return;
        if (run.getAgentId() != null && clean.equals(run.getAgentId())) return;
        Optional<Agent> agentOpt = agentRepository.findById(run.getAgentId());
        if (agentOpt.isPresent() && agentOpt.get().getOwnerHumanId() != null
                && clean.equals(agentOpt.get().getOwnerHumanId())) {
            return;
        }
        throw new IllegalStateException(
            "Only the run's agent, its owner, or an admin may " + action + " a run: " + run.getId());
    }

    @Transactional
    public Run complete(String id, String result, Long costTokens, Double costUsd) {
        return complete(id, result, costTokens, costUsd, Defaults.SYSTEM_ACTOR);
    }

    @Transactional
    public Run complete(String id, String result, Long costTokens, Double costUsd, String actor) {
        Run run = runRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + id));
        requireRunAuthority(run, actor, "complete");
        if (!"running".equals(run.getStatus())) {
            throw new IllegalStateException("Cannot complete run with status: " + run.getStatus());
        }
        run.setStatus("completed");
        run.setResult(result);
        run.setCompletedAt(Instant.now());
        if (costTokens != null) run.setCostTokens(costTokens);
        if (costUsd != null) run.setCostUsd(costUsd);
        Run saved = runRepository.save(run);
        // SPW-060: land a settle line so the spend breaker sees API-landed runs,
        // matching NodeService.reportRun (the node path lands the same ledger row).
        // spend_ledger.member_id carries the agent id (its agents(id) FK); the agent is
        // accountable for its own spend, and the run already references it.
        if (costUsd != null && costUsd > 0 && run.getAgentId() != null) {
            SpendLedger ledger = new SpendLedger();
            ledger.setId(UUID.randomUUID().toString());
            ledger.setMemberId(run.getAgentId());
            ledger.setRunId(run.getId());
            ledger.setKind("settle");
            ledger.setCost(costUsd);
            ledger.setTokensOut(costTokens != null ? costTokens : 0L);
            spendLedgerRepository.save(ledger);
        }
        auditService.logSystem("COMPLETE_RUN", "run", id, null);
        return saved;
    }

    @Transactional
    public Run fail(String id, String errorMessage) {
        return fail(id, errorMessage, Defaults.SYSTEM_ACTOR);
    }

    @Transactional
    public Run fail(String id, String errorMessage, String actor) {
        Run run = runRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + id));
        requireRunAuthority(run, actor, "fail");
        if (!"running".equals(run.getStatus())) {
            throw new IllegalStateException("Cannot fail run with status: " + run.getStatus());
        }
        run.setStatus("failed");
        run.setErrorMessage(errorMessage);
        run.setCompletedAt(Instant.now());
        Run saved = runRepository.save(run);
        auditService.logSystem("FAIL_RUN", "run", id,
            String.format("{\"error\":%s}", JsonHelpers.jsonString(errorMessage != null ? errorMessage.substring(0, Math.min(200, errorMessage.length())) : "")));
        return saved;
    }

    @Transactional
    public Run cancel(String id) {
        return cancel(id, Defaults.SYSTEM_ACTOR);
    }

    @Transactional
    public Run cancel(String id, String actor) {
        Run run = runRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + id));
        requireRunAuthority(run, actor, "cancel");
        if (!"queued".equals(run.getStatus()) && !"running".equals(run.getStatus())) {
            throw new IllegalStateException("Cannot cancel run with status: " + run.getStatus());
        }
        run.setStatus("cancelled");
        run.setCompletedAt(Instant.now());
        Run saved = runRepository.save(run);
        auditService.logSystem("CANCEL_RUN", "run", id, null);
        return saved;
    }

    @Transactional
    public Run suspend(String id) {
        Run run = runRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + id));
        if (!"running".equals(run.getStatus())) {
            throw new IllegalStateException("Cannot suspend run with status: " + run.getStatus());
        }
        run.setStatus("suspended");
        Run saved = runRepository.save(run);
        auditService.logSystem("SUSPEND_RUN", "run", id, null);
        return saved;
    }

    /**
     * CLC-032: "Resume ... never resurrects a halted one — a run suspended mid-flight
     * is terminal: partial results fold back, interrupted work re-enters as new runs
     * or board tasks, staged writes resolve through reconciliation — no half-replayed
     * side effect." NFR-011: "no backward clock step un-expires anything or reverses a
     * terminal transition."
     *
     * <p>The suspended run stays suspended; the caller files new work instead.
     */
    @Transactional
    public Run resume(String id) {
        Run run = runRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + id));
        if (!"suspended".equals(run.getStatus())) {
            throw new IllegalStateException("Cannot resume run with status: " + run.getStatus());
        }
        throw new IllegalStateException(
                "A suspended run is terminal (CLC-032) — it is never resurrected. "
                    + "Re-enter the interrupted work as a new run or a board task.");
    }

    @Transactional(readOnly = true)
    public boolean agentExists(String agentId) {
        return agentRepository.existsById(agentId);
    }

    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return runRepository.countByStatus(status);
    }
}
