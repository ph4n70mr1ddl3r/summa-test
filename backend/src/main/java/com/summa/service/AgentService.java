package com.summa.service;

import com.summa.repository.AgentRepository;
import com.summa.model.Agent;
import com.summa.repository.AskRepository;
import com.summa.model.Ask;
import com.summa.repository.BoardTaskRepository;
import com.summa.repository.InitiativeRepository;
import com.summa.repository.TriggerRepository;
import com.summa.repository.SpawnRequestRepository;
import com.summa.repository.RunRepository;
import com.summa.repository.DnaProposalRepository;
import com.summa.model.BoardTask;
import com.summa.model.Initiative;
import com.summa.model.Trigger;
import com.summa.model.SpawnRequest;
import com.summa.model.Run;
import com.summa.model.DnaProposal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.summa.model.Human;
import com.summa.enums.AgentStatus;
import com.summa.util.JsonHelpers;
import com.summa.constants.Defaults;
import com.summa.exception.EntityNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AgentService {
    private final AgentRepository agentRepository;
    private final AuditService auditService;
    private final MemberService memberService;
    private final AskRepository askRepository;
    private final BoardTaskRepository boardTaskRepository;
    private final InitiativeRepository initiativeRepository;
    private final TriggerRepository triggerRepository;
    private final SpawnRequestRepository spawnRequestRepository;
    private final RunRepository runRepository;
    private final DnaProposalRepository proposalRepository;
    private final int depthCap;
    private final ObjectMapper objectMapper;
    private final SpendLedgerService spendLedgerService;

    public AgentService(AgentRepository agentRepository, AuditService auditService,
                        MemberService memberService, AskRepository askRepository,
                        BoardTaskRepository boardTaskRepository,
                        InitiativeRepository initiativeRepository,
                        TriggerRepository triggerRepository,
                        SpawnRequestRepository spawnRequestRepository,
                        RunRepository runRepository,
                        DnaProposalRepository proposalRepository,
                        ObjectMapper objectMapper,
                        @Value("${summa.spawn.depth-cap:2}") int depthCap) {
        this(agentRepository, auditService, memberService, askRepository,
             boardTaskRepository, initiativeRepository, triggerRepository,
             spawnRequestRepository, runRepository, proposalRepository,
             objectMapper, depthCap, null);
    }

    public AgentService(AgentRepository agentRepository, AuditService auditService,
                        MemberService memberService, AskRepository askRepository,
                        BoardTaskRepository boardTaskRepository,
                        InitiativeRepository initiativeRepository,
                        TriggerRepository triggerRepository,
                        SpawnRequestRepository spawnRequestRepository,
                        RunRepository runRepository,
                        DnaProposalRepository proposalRepository,
                        ObjectMapper objectMapper,
                        @Value("${summa.spawn.depth-cap:2}") int depthCap,
                        SpendLedgerService spendLedgerService) {
        this.agentRepository = agentRepository;
        this.auditService = auditService;
        this.memberService = memberService;
        this.askRepository = askRepository;
        this.boardTaskRepository = boardTaskRepository;
        this.initiativeRepository = initiativeRepository;
        this.triggerRepository = triggerRepository;
        this.spawnRequestRepository = spawnRequestRepository;
        this.runRepository = runRepository;
        this.proposalRepository = proposalRepository;
        this.objectMapper = objectMapper;
        this.depthCap = Math.max(2, depthCap);
        this.spendLedgerService = spendLedgerService;
    }

    @Transactional
    public Agent create(String id, String name, String ownerHumanId, String agentClass,
                        String spawnedBy, Integer lineageDepth, String templateId,
                        String templateVersion, Double budgetCap, Instant ttlAt) {
        Agent agent = new Agent();
        agent.setId(id);
        agent.setName(name);
        agent.setOwnerHumanId(ownerHumanId);
        agent.setAgentClass(agentClass);
        agent.setSpawnedBy(spawnedBy);
        agent.setLineageDepth(lineageDepth != null ? lineageDepth : 0);
        agent.setTemplateId(templateId);
        agent.setTemplateVersion(templateVersion);
        agent.setBudgetCap(budgetCap);
        agent.setTtlAt(ttlAt);

        Agent saved = agentRepository.save(agent);
        auditService.log(ownerHumanId, "CREATE_AGENT", "agent", id,
            String.format("{\"name\":%s,\"class\":%s}", JsonHelpers.jsonString(name), JsonHelpers.jsonString(agentClass)));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Agent> findById(String id) {
        return agentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Agent> findAll() {
        return agentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Agent> findAllActive() {
        return agentRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public List<Agent> findAllActive(int limit) {
        return agentRepository.findAllActiveOrdered(limit);
    }

    @Transactional
    public Agent suspend(String id, String actor) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agent not found: " + id));

        if (!AgentStatus.ACTIVE.getValue().equals(agent.getStatus())) {
            throw new IllegalStateException("Agent is not active: " + agent.getStatus());
        }

        agent.setStatus(AgentStatus.SUSPENDED.getValue());
        agent.setSuspendedAt(Instant.now());
        Agent saved = agentRepository.save(agent);

        // CLC-030: Suspend halts in-flight runs — partial results fold back, never killed mid-commit
        for (Run run : findRunningRuns(id)) {
            run.setStatus("suspended");
            runRepository.save(run);
            auditService.logSystem("SUSPEND_HALT_RUN", "run", run.getId(),
                String.format("{\"agentId\":%s,\"reason\":\"agent_suspended\"}", JsonHelpers.jsonString(id)));
        }

        auditService.log(actor, "SUSPEND", "agent", id, null);
        return saved;
    }

    private List<Run> findRunningRuns(String agentId) {
        return runRepository.findByAgentIdAndStatus(agentId, "running");
    }

    @Transactional
    public Agent resume(String id, String actor) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agent not found: " + id));

        if (!AgentStatus.SUSPENDED.getValue().equals(agent.getStatus())) {
            throw new IllegalStateException("Agent is not suspended: " + agent.getStatus());
        }

        agent.setStatus(AgentStatus.ACTIVE.getValue());
        agent.setSuspendedAt(null);
        Agent saved = agentRepository.save(agent);
        auditService.log(actor, "RESUME", "agent", id, null);
        return saved;
    }

    @Transactional
    public Agent retire(String id, String actor) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agent not found: " + id));

        if (!AgentStatus.ACTIVE.getValue().equals(agent.getStatus())) {
            throw new IllegalStateException("Can only retire active agents, current status: " + agent.getStatus());
        }

        // CLC-020: full dependency walk on retire
        // DAT-120: asks.from/asks.to and board_tasks.assignee_member_id are keyed
        // unions (a:<id> for agent refs), so sweep both bare and a:-prefixed forms.
        String agentKeyedRef = "a:" + id;
        // 1. Close pending asks from the retiring agent
        for (Ask ask : askRepository.findByFromAndStatusPending(id)) {
            ask.setStatus("withdrawn");
            askRepository.save(ask);
            auditService.logSystem("RETIRE_CLOSE_ASK_FROM", "ask", ask.getId(),
                String.format("{\"agentId\":%s,\"reason\":\"agent_retiring\"}", JsonHelpers.jsonString(id)));
        }
        for (Ask ask : askRepository.findByFromAndStatusPending(agentKeyedRef)) {
            ask.setStatus("withdrawn");
            askRepository.save(ask);
            auditService.logSystem("RETIRE_CLOSE_ASK_FROM", "ask", ask.getId(),
                String.format("{\"agentId\":%s,\"reason\":\"agent_retiring\"}", JsonHelpers.jsonString(id)));
        }

        // ASK-061: Reassign asks TO the retiring agent up the chain
        // (non-active target rule reassigns them to deputy or admin broadcast)
        for (Ask ask : askRepository.findByToAndStatusPending(id)) {
            reassignRetiringAgentAsk(ask, id, targetFallbackFor(id));
        }
        for (Ask ask : askRepository.findByToAndStatusPending(agentKeyedRef)) {
            reassignRetiringAgentAsk(ask, id, OffboardingWalkService.ADMIN_BROADCAST);
        }

        // 2. Cancel board tasks assigned to the agent
        for (BoardTask task : boardTaskRepository.findByAssigneeMemberId(id)) {
            cancelRetiringAgentTask(task, id);
        }
        for (BoardTask task : boardTaskRepository.findByAssigneeMemberId(agentKeyedRef)) {
            cancelRetiringAgentTask(task, id);
        }

        // 3. Archive pending spawn requests from the agent (DAT-120: bare + a: forms)
        for (String requesterRef : List.of(id, agentKeyedRef)) {
            for (SpawnRequest spawn : spawnRequestRepository.findByRequesterId(requesterRef)) {
                if ("requested".equals(spawn.getStatus())) {
                    spawn.setStatus("archived");
                    spawnRequestRepository.save(spawn);
                    if (spendLedgerService != null) {
                        spendLedgerService.releaseSpawn(spawn, "system");
                    }
                    auditService.logSystem("RETIRE_ARCHIVE_SPAWN", "spawn_request", spawn.getId(),
                        String.format("{\"agentId\":%s,\"reason\":\"agent_retiring\"}", JsonHelpers.jsonString(id)));
                }
            }
        }

        // 4. Pause active triggers owned by the agent
        for (Trigger trigger : triggerRepository.findByAgentId(id)) {
            if ("active".equals(trigger.getStatus())) {
                trigger.setStatus("paused");
                triggerRepository.save(trigger);
                auditService.logSystem("RETIRE_PAUSE_TRIGGER", "trigger", trigger.getId(),
                    String.format("{\"agentId\":%s,\"reason\":\"agent_retiring\"}", JsonHelpers.jsonString(id)));
            }
        }

        // 5. Pause initiatives the agent sponsors or leads (CLC-020: both posts; DAT-120: bare + a: refs)
        Set<String> seenInitiatives = new HashSet<>();
        for (String ref : List.of(id, agentKeyedRef)) {
            for (Initiative init : initiativeRepository.findBySponsor(ref)) {
                if (seenInitiatives.add(init.getId())) {
                    pauseRetiringAgentInitiative(init, id);
                }
            }
            for (Initiative init : initiativeRepository.findByLead(ref)) {
                if (seenInitiatives.add(init.getId())) {
                    pauseRetiringAgentInitiative(init, id);
                }
            }
        }

        // CLC-026: Withdraw open DNA proposals authored by the retiring agent
        for (DnaProposal prop : proposalRepository.findByProposedByAndStatus(id, "open")) {
            prop.setStatus("withdrawn");
            proposalRepository.save(prop);
            auditService.logSystem("RETIRE_WITHDRAW_PROPOSAL", "dna_proposal", prop.getId(),
                String.format("{\"agentId\":%s,\"reason\":\"agent_retiring\"}", JsonHelpers.jsonString(id)));
        }
        for (DnaProposal prop : proposalRepository.findByProposedByAndStatus(agentKeyedRef, "open")) {
            prop.setStatus("withdrawn");
            proposalRepository.save(prop);
            auditService.logSystem("RETIRE_WITHDRAW_PROPOSAL", "dna_proposal", prop.getId(),
                String.format("{\"agentId\":%s,\"reason\":\"agent_retiring\"}", JsonHelpers.jsonString(id)));
        }

        // CLC-030: Cancel in-flight runs — consistent with suspend behavior
        for (Run run : findRunningRuns(id)) {
            run.setStatus("cancelled");
            run.setCompletedAt(Instant.now());
            runRepository.save(run);
            auditService.logSystem("RETIRE_CANCEL_RUN", "run", run.getId(),
                String.format("{\"agentId\":%s,\"reason\":\"agent_retiring\"}", JsonHelpers.jsonString(id)));
        }

        agent.setStatus(AgentStatus.RETIRING.getValue());
        agent.setRetiredAt(Instant.now());
        Agent saved = agentRepository.save(agent);
        auditService.log(actor, "RETIRE", "agent", id, null);
        return saved;
    }

    private String targetFallbackFor(String agentId) {
        Optional<String> deputy = memberService.resolveDeputyTarget(agentId);
        return deputy.orElse(OffboardingWalkService.ADMIN_BROADCAST);
    }

    private void reassignRetiringAgentAsk(Ask ask, String agentId, String newTo) {
        ask.setTo(newTo);
        askRepository.save(ask);
        auditService.logSystem("RETIRE_REASSIGN_ASK_TO", "ask", ask.getId(),
            String.format("{\"agentId\":%s,\"newTo\":%s,\"reason\":\"agent_retiring\"}",
                JsonHelpers.jsonString(agentId), JsonHelpers.jsonString(newTo)));
    }

    private void cancelRetiringAgentTask(BoardTask task, String agentId) {
        if ("open".equals(task.getStatus()) || "in_progress".equals(task.getStatus())) {
            task.setStatus("cancelled");
            boardTaskRepository.save(task);
            auditService.logSystem("RETIRE_CANCEL_TASK", "board_task", task.getId(),
                String.format("{\"agentId\":%s,\"reason\":\"agent_retiring\"}", JsonHelpers.jsonString(agentId)));
        }
    }

    private void pauseRetiringAgentInitiative(Initiative init, String agentId) {
        if ("active".equals(init.getStatus()) || "paused".equals(init.getStatus())) {
            init.setStatus("paused");
            initiativeRepository.save(init);
            auditService.logSystem("RETIRE_PAUSE_INITIATIVE", "initiative", init.getId(),
                String.format("{\"agentId\":%s,\"reason\":\"agent_retiring\"}", JsonHelpers.jsonString(agentId)));
        }
    }

    @Transactional
    public Agent archive(String id, String actor) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agent not found: " + id));

        if (!AgentStatus.ACTIVE.getValue().equals(agent.getStatus())
                && !AgentStatus.SUSPENDED.getValue().equals(agent.getStatus())) {
            throw new IllegalStateException("Only active or suspended agents can be archived");
        }
        agent.setStatus(AgentStatus.ARCHIVED.getValue());
        agent.setArchivedAt(Instant.now());
        Agent saved = agentRepository.save(agent);
        auditService.log(actor, "ARCHIVE", "agent", id, null);
        return saved;
    }

    /**
     * CLC-002: Denial is terminal for requested rows — requested→archived.
     * A requested agent that is denied never becomes active.
     * Before archiving, sweep dependencies that could reference the agent
     * (pending spawn requests it filed, active triggers it owns), matching
     * the retire/cleanupSuspendedAgent walks so no live state is stranded.
     */
    @Transactional
    public Agent deny(String id, String actor) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agent not found: " + id));

        if (!AgentStatus.REQUESTED.getValue().equals(agent.getStatus())) {
            throw new IllegalStateException("Can only deny requested agents, current status: " + agent.getStatus());
        }

        // Archive pending spawn requests this agent filed (DAT-120: sweep bare and a: forms)
        for (String requesterRef : List.of(id, "a:" + id)) {
            for (SpawnRequest spawn : spawnRequestRepository.findByRequesterId(requesterRef)) {
                if ("requested".equals(spawn.getStatus())) {
                    spawn.setStatus("archived");
                    spawnRequestRepository.save(spawn);
                    auditService.logSystem("DENY_ARCHIVE_SPAWN", "spawn_request", spawn.getId(),
                        String.format("{\"agentId\":%s,\"reason\":\"agent_denied\"}", JsonHelpers.jsonString(id)));
                }
            }
        }

        // Pause active triggers owned by the agent
        for (Trigger trigger : triggerRepository.findByAgentId(id)) {
            if ("active".equals(trigger.getStatus())) {
                trigger.setStatus("paused");
                triggerRepository.save(trigger);
                auditService.logSystem("DENY_PAUSE_TRIGGER", "trigger", trigger.getId(),
                    String.format("{\"agentId\":%s,\"reason\":\"agent_denied\"}", JsonHelpers.jsonString(id)));
            }
        }

        agent.setStatus(AgentStatus.ARCHIVED.getValue());
        agent.setArchivedAt(Instant.now());
        Agent saved = agentRepository.save(agent);
        auditService.log(actor, "DENY", "agent", id, null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Agent> findByOwner(String ownerHumanId) {
        return agentRepository.findByOwner(ownerHumanId);
    }

    @Transactional(readOnly = true)
    public List<Agent> findByOwner(String ownerHumanId, int limit) {
        return agentRepository.findByOwnerOrdered(ownerHumanId, limit);
    }

    @Transactional(readOnly = true)
    public List<Agent> findByOwnerActive(String ownerHumanId) {
        return agentRepository.findActiveByOwner(ownerHumanId);
    }

    @Transactional(readOnly = true)
    public List<Agent> findByStatus(String status) {
        return agentRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Agent> findByStatus(String status, int limit) {
        return agentRepository.findByStatusOrdered(status, limit);
    }

    public int getDepthCap() {
        return depthCap;
    }

    public Optional<Human> findFirstHumanUpChain(String agentId) {
        String currentId = agentId;
        int depth = 0;
        while (depth < Defaults.MAX_LINEAGE_CHAIN_DEPTH) {
            Optional<Agent> agent = agentRepository.findById(currentId);
            if (agent.isEmpty()) break;
            Optional<Human> owner = memberService.findHuman(agent.get().getOwnerHumanId());
            if (owner.isPresent()) return owner;
            currentId = agent.get().getSpawnedBy();
            if (currentId == null) break;
            depth++;
        }
        return Optional.empty();
    }

    /**
     * SPW-071: TTL reaper — grace window before killing mid-write; suspended workers halt-then-reap.
     * Runs every 5 minutes; reaps agents whose ttl_at has passed.
     * Each agent is processed in its own transaction to isolate failures.
     */
    @Scheduled(fixedRate = Defaults.TTL_REAP_INTERVAL_MS)
    public void reapExpiredAgents() {
        Instant now = Instant.now();
        List<Agent> activeExpired = agentRepository.findActiveExpiredBefore(now);
        List<Agent> suspendedExpired = agentRepository.findSuspendedExpiredBefore(now);
        for (Agent agent : activeExpired) {
            try {
                // Re-fetch to avoid lost-update from concurrent modifications
                Optional<Agent> fresh = agentRepository.findById(agent.getId());
                if (fresh.isEmpty()) continue;
                Agent current = fresh.get();
                if (!AgentStatus.ACTIVE.getValue().equals(current.getStatus())) continue;
                retire(current.getId(), "system");
            } catch (Exception e) {
                auditService.logSystem("TTL_REAP_FAIL", "agent", agent.getId(),
                    JsonHelpers.toJson(Map.of("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), objectMapper));
            }
        }
        for (Agent agent : suspendedExpired) {
            try {
                // Re-fetch to avoid lost-update from concurrent modifications
                Optional<Agent> fresh = agentRepository.findById(agent.getId());
                if (fresh.isEmpty()) continue;
                Agent current = fresh.get();
                if (!AgentStatus.SUSPENDED.getValue().equals(current.getStatus())) continue;
                cleanupSuspendedAgent(current);
            } catch (Exception e) {
                auditService.logSystem("TTL_REAP_SUSPENDED_FAIL", "agent", agent.getId(),
                    JsonHelpers.toJson(Map.of("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), objectMapper));
            }
        }
    }

    /**
     * CLC-002: SUSPENDED-002: dependency cleanup for a suspended agent before archival.
     * Each repository save auto-commits; the outer reapExpiredAgents caller isolates
     * per-agent failures with try/catch.
     */
    @Transactional
    private void cleanupSuspendedAgent(Agent agent) {
        String agentId = agent.getId();
        String agentKeyedRef = "a:" + agentId;
        for (Ask ask : askRepository.findByFromAndStatusPending(agentId)) {
            ask.setStatus("withdrawn");
            askRepository.save(ask);
        }
        for (Ask ask : askRepository.findByFromAndStatusPending(agentKeyedRef)) {
            ask.setStatus("withdrawn");
            askRepository.save(ask);
        }
        for (Ask ask : askRepository.findByToAndStatusPending(agentId)) {
            ask.setStatus("withdrawn");
            askRepository.save(ask);
        }
        for (Ask ask : askRepository.findByToAndStatusPending(agentKeyedRef)) {
            ask.setStatus("withdrawn");
            askRepository.save(ask);
        }
        for (SpawnRequest spawn : spawnRequestRepository.findByRequesterId(agentId)) {
            if (AgentStatus.REQUESTED.getValue().equals(spawn.getStatus())) {
                spawn.setStatus("archived");
                spawnRequestRepository.save(spawn);
            }
        }
        // Ensure initiative references are cleaned up (mirrors active-agent retire behavior).
        // DAT-120: initiatives.sponsor/lead are keyed unions (h:<id>/a:<id>); the admin
        // broadcast addressee 'admins' is reserved for asks.to alone, so custody falls to a
        // concrete active admin (h:-prefixed). If none exists, the row keeps the agent ref
        // rather than writing a value the CHECK constraint would reject.
        String adminCustody = memberService.findAdmins().stream()
            .map(h -> "h:" + h.getId())
            .findFirst()
            .orElse(null);
        List<Initiative> owned = new ArrayList<>(initiativeRepository.findBySponsor(agentId));
        owned.addAll(initiativeRepository.findBySponsor(agentKeyedRef));
        owned.addAll(initiativeRepository.findByLead(agentId));
        owned.addAll(initiativeRepository.findByLead(agentKeyedRef));
        Set<String> seen = new HashSet<>();
        for (Initiative init : owned) {
            if (!seen.add(init.getId())) continue;
            boolean changed = false;
            if (adminCustody != null
                    && (Objects.equals(agentId, init.getSponsor()) || Objects.equals(agentKeyedRef, init.getSponsor()))) {
                init.setSponsor(adminCustody);
                changed = true;
            }
            if (adminCustody != null
                    && (Objects.equals(agentId, init.getLead()) || Objects.equals(agentKeyedRef, init.getLead()))) {
                init.setLead(adminCustody);
                changed = true;
            }
            if (changed) {
                initiativeRepository.save(init);
            }
        }
        for (Trigger trigger : triggerRepository.findByAgentId(agentId)) {
            if ("active".equals(trigger.getStatus())) {
                trigger.setStatus("paused");
                triggerRepository.save(trigger);
            }
        }
        agent.setStatus(AgentStatus.ARCHIVED.getValue());
        agent.setArchivedAt(Instant.now());
        agentRepository.save(agent);
        auditService.logSystem("TTL_REAP_SUSPENDED", "agent", agent.getId(),
            "{\"reason\":\"ttl_expired\"}");
    }
}
