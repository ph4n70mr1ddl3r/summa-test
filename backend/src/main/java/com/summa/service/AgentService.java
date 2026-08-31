package com.summa.service;

import com.summa.repository.AgentRepository;
import com.summa.model.Agent;
import com.summa.repository.AskRepository;
import com.summa.model.Ask;
import com.summa.repository.BoardTaskRepository;
import com.summa.repository.InitiativeRepository;
import com.summa.repository.TriggerRepository;
import com.summa.repository.SpawnRequestRepository;
import com.summa.model.BoardTask;
import com.summa.model.Initiative;
import com.summa.model.Trigger;
import com.summa.model.SpawnRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.summa.model.Human;

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
    private final int depthCap;

    public AgentService(AgentRepository agentRepository, AuditService auditService,
                        MemberService memberService, AskRepository askRepository,
                        BoardTaskRepository boardTaskRepository,
                        InitiativeRepository initiativeRepository,
                        TriggerRepository triggerRepository,
                        SpawnRequestRepository spawnRequestRepository,
                        @Value("${summa.spawn.depth-cap:2}") int depthCap) {
        this.agentRepository = agentRepository;
        this.auditService = auditService;
        this.memberService = memberService;
        this.askRepository = askRepository;
        this.boardTaskRepository = boardTaskRepository;
        this.initiativeRepository = initiativeRepository;
        this.triggerRepository = triggerRepository;
        this.spawnRequestRepository = spawnRequestRepository;
        this.depthCap = depthCap;
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
            String.format("{\"name\":\"%s\",\"class\":\"%s\"}", name, agentClass));
        return saved;
    }

    public Optional<Agent> findById(String id) {
        return agentRepository.findById(id);
    }

    public List<Agent> findAll() {
        return agentRepository.findAll();
    }

    public List<Agent> findAllActive() {
        return agentRepository.findAllActive();
    }

    @Transactional
    public Agent suspend(String id, String actor) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));

        if (!"active".equals(agent.getStatus())) {
            throw new IllegalStateException("Agent is not active: " + agent.getStatus());
        }

        agent.setStatus("suspended");
        agent.setSuspendedAt(Instant.now());
        Agent saved = agentRepository.save(agent);
        auditService.log(actor, "SUSPEND", "agent", id, null);
        return saved;
    }

    @Transactional
    public Agent resume(String id, String actor) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));

        if (!"suspended".equals(agent.getStatus())) {
            throw new IllegalStateException("Agent is not suspended: " + agent.getStatus());
        }

        agent.setStatus("active");
        agent.setSuspendedAt(null);
        Agent saved = agentRepository.save(agent);
        auditService.log(actor, "RESUME", "agent", id, null);
        return saved;
    }

    @Transactional
    public Agent retire(String id, String actor) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));

        if (!"active".equals(agent.getStatus())) {
            throw new IllegalStateException("Can only retire active agents, current status: " + agent.getStatus());
        }

        // CLC-020: full dependency walk on retire
        // 1. Close pending asks from the retiring agent
        for (Ask ask : askRepository.findByFromAndStatusPending(id)) {
            ask.setStatus("withdrawn");
            askRepository.save(ask);
            auditService.logSystem("RETIRE_CLOSE_ASK_FROM", "ask", ask.getId(),
                String.format("{\"agentId\":\"%s\",\"reason\":\"agent_retiring\"}", id));
        }

        // 2. Cancel board tasks assigned to the agent
        for (BoardTask task : boardTaskRepository.findByAssigneeMemberId(id)) {
            if ("open".equals(task.getStatus()) || "in_progress".equals(task.getStatus())) {
                task.setStatus("cancelled");
                boardTaskRepository.save(task);
                auditService.logSystem("RETIRE_CANCEL_TASK", "board_task", task.getId(),
                    String.format("{\"agentId\":\"%s\",\"reason\":\"agent_retiring\"}", id));
            }
        }

        // 3. Archive pending spawn requests from the agent
        for (SpawnRequest spawn : spawnRequestRepository.findByRequesterId(id)) {
            if ("requested".equals(spawn.getStatus())) {
                spawn.setStatus("archived");
                spawnRequestRepository.save(spawn);
                auditService.logSystem("RETIRE_ARCHIVE_SPAWN", "spawn_request", spawn.getId(),
                    String.format("{\"agentId\":\"%s\",\"reason\":\"agent_retiring\"}", id));
            }
        }

        // 4. Pause active triggers owned by the agent
        for (Trigger trigger : triggerRepository.findByAgentId(id)) {
            if ("active".equals(trigger.getStatus())) {
                trigger.setStatus("paused");
                triggerRepository.save(trigger);
                auditService.logSystem("RETIRE_PAUSE_TRIGGER", "trigger", trigger.getId(),
                    String.format("{\"agentId\":\"%s\",\"reason\":\"agent_retiring\"}", id));
            }
        }

        // 5. Pause initiatives led by the agent
        for (Initiative init : initiativeRepository.findByLead(id)) {
            if ("active".equals(init.getStatus()) || "paused".equals(init.getStatus())) {
                init.setStatus("paused");
                initiativeRepository.save(init);
                auditService.logSystem("RETIRE_PAUSE_INITIATIVE", "initiative", init.getId(),
                    String.format("{\"agentId\":\"%s\",\"reason\":\"agent_retiring\"}", id));
            }
        }

        agent.setStatus("retiring");
        agent.setRetiredAt(Instant.now());
        Agent saved = agentRepository.save(agent);
        auditService.log(actor, "RETIRE", "agent", id, null);
        return saved;
    }

    @Transactional
    public Agent archive(String id, String actor) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));

        agent.setStatus("archived");
        agent.setArchivedAt(Instant.now());
        Agent saved = agentRepository.save(agent);
        auditService.log(actor, "ARCHIVE", "agent", id, null);
        return saved;
    }

    public long countActiveAgents() {
        return agentRepository.countActiveAgents();
    }

    public List<Agent> findByOwner(String ownerHumanId) {
        return agentRepository.findActiveByOwner(ownerHumanId);
    }

    public List<Agent> findByStatus(String status) {
        return agentRepository.findByStatus(status);
    }

    public List<Agent> findChildren(String parentId) {
        return agentRepository.findBySpawnedBy(parentId);
    }

    public int getDepthCap() {
        return depthCap;
    }

    public Optional<Human> findFirstHumanUpChain(String agentId) {
        String currentId = agentId;
        int depth = 0;
        while (depth < depthCap) {
            Optional<Agent> agent = agentRepository.findById(currentId);
            if (agent.isEmpty()) break;
            Optional<Human> owner = memberService.findHuman(agent.get().getOwnerHumanId());
            if (owner.isPresent()) return owner;
            currentId = agent.get().getSpawnedBy();
            depth++;
        }
        return Optional.empty();
    }
}
