package com.summa.service;

import com.summa.repository.AgentRepository;
import com.summa.model.Agent;
import com.summa.repository.AskRepository;
import com.summa.model.Ask;
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
    private final int depthCap;

    public AgentService(AgentRepository agentRepository, AuditService auditService,
                        MemberService memberService, AskRepository askRepository,
                        @Value("${summa.spawn.depth-cap:2}") int depthCap) {
        this.agentRepository = agentRepository;
        this.auditService = auditService;
        this.memberService = memberService;
        this.askRepository = askRepository;
        this.depthCap = depthCap;
    }

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

        // CLC-020: Resolve pending asks from the retiring agent — close with audit note
        for (Ask ask : askRepository.findByFromAndStatusPending(id)) {
            ask.setStatus("withdrawn");
            askRepository.save(ask);
            auditService.logSystem("RETIRE_CLOSE_ASK_FROM", "ask", ask.getId(),
                String.format("{\"agentId\":\"%s\",\"reason\":\"agent_retiring\"}", id));
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

    public List<Agent> findChildren(String parentId) {
        return agentRepository.findBySpawnedBy(parentId);
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
