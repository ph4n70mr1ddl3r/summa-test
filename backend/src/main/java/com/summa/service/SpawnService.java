package com.summa.service;

import com.summa.repository.SpawnRequestRepository;
import com.summa.model.SpawnRequest;
import com.summa.repository.RoleTemplateRepository;
import com.summa.model.RoleTemplate;
import com.summa.repository.AgentRepository;
import com.summa.model.Agent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;

@Service
public class SpawnService {
    private final SpawnRequestRepository spawnRepository;
    private final AuditService auditService;
    private final GovernanceService governanceService;
    private final RoleTemplateRepository templateRepository;
    private final AgentRepository agentRepository;

    @Value("${summa.spawn.depth-cap:2}")
    private int depthCap;

    public SpawnService(SpawnRequestRepository spawnRepository, AuditService auditService,
                          GovernanceService governanceService, RoleTemplateRepository templateRepository,
                          AgentRepository agentRepository) {
        this.spawnRepository = spawnRepository;
        this.auditService = auditService;
        this.governanceService = governanceService;
        this.templateRepository = templateRepository;
        this.agentRepository = agentRepository;
        this.depthCap = Math.max(2, depthCap);
    }

    @Transactional
    public SpawnRequest create(String requesterId, String templateId, String customRole,
                                   String spawnClass, String purpose, String workspaceBindings,
                                   String scopeCeiling, Double budgetCap, Integer ttlHours,
                                   String requestedByHumanId, String actor) {
        // SPW-001: Validate requester is an existing active agent
        Optional<Agent> requesterOpt = agentRepository.findById(requesterId);
        if (requesterOpt.isEmpty()) {
            throw new IllegalArgumentException("Requester agent not found: " + requesterId);
        }
        if (!"active".equals(requesterOpt.get().getStatus())) {
            throw new IllegalStateException("Requester agent is not active: " + requesterId);
        }

        // SPW-060: Check spend halt
        if (governanceService.isSpendHaltTripped()) {
            throw new IllegalStateException("Spend halt is active - spawns are blocked");
        }

        // SPW-021: Class must match — persistent hire needs persistent template,
        // ephemeral needs ephemeral-subagent template
        String effectiveSpawnClass = spawnClass != null ? spawnClass : "ephemeral";
        if (templateId != null && !templateId.isBlank()) {
            Optional<RoleTemplate> templateOpt = templateRepository.findById(templateId);
            if (templateOpt.isEmpty()) {
                throw new IllegalArgumentException("Template not found: " + templateId);
            }
            RoleTemplate template = templateOpt.get();
            if (!template.isActive()) {
                throw new IllegalStateException("Template is not active: " + templateId
                    + " (status: " + template.getStatus() + ")");
            }
            String requiredClass = "ephemeral".equals(effectiveSpawnClass) ? "ephemeral-subagent" : "persistent";
            if (!requiredClass.equals(template.getAgentClass())) {
                throw new IllegalStateException("Template class mismatch: request class="
                    + effectiveSpawnClass + " but template class=" + template.getAgentClass());
            }
        }

        SpawnRequest request = new SpawnRequest();
        request.setId(UUID.randomUUID().toString());
        request.setRequesterId(requesterId);
        request.setTemplateId(templateId);
        request.setCustomRole(customRole);
        request.setSpawnClass(effectiveSpawnClass);
        request.setPurpose(purpose);
        request.setWorkspaceBindings(workspaceBindings != null ? workspaceBindings : "[]");
        request.setScopeCeiling(scopeCeiling != null ? scopeCeiling : "{}");
        request.setBudgetCap(budgetCap);
        request.setTtlHours(ttlHours);
        request.setRequestedByHumanId(requestedByHumanId);
        request.setStatus("requested");

        SpawnRequest saved = spawnRepository.save(request);
        auditService.log(actor, "CREATE_SPAWN", "spawn_request", saved.getId(),
            String.format("{\"class\":\"%s\",\"templateId\":\"%s\"}", effectiveSpawnClass, templateId));
        return saved;
    }

    public Optional<SpawnRequest> findById(String id) {
        return spawnRepository.findById(id);
    }

    public List<SpawnRequest> findByStatus(String status) {
        return spawnRepository.findByStatus(status);
    }

    public List<SpawnRequest> findByRequester(String requesterId) {
        return spawnRepository.findByRequesterId(requesterId);
    }

    @Transactional
    public SpawnRequest approve(String id, String approvedBy, String actor) {
        SpawnRequest request = spawnRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Spawn request not found: " + id));

        if (!"requested".equals(request.getStatus())) {
            throw new IllegalStateException("Cannot approve non-requested spawn: " + request.getStatus());
        }

        // SPW-062: Check spend halt — accept under halt is audit-only, return with distinct status
        if (governanceService.isSpendHaltTripped()) {
            request.setStatus("archived");
            request.setApprovedBy(approvedBy);
            request.setApprovedAt(Instant.now());
            SpawnRequest saved = spawnRepository.save(request);
            auditService.log(actor, "AUDIT_ONLY_SPAWN_APPROVE", "spawn_request", id,
                "Rejected: spend halt active");
            return saved;
        }

        request.setStatus("approved");
        request.setApprovedBy(approvedBy);
        request.setApprovedAt(Instant.now());

        // SPW-011: Approved spawn creates an active agent
        Agent agent = activateAgent(request, actor);
        request.setAgentId(agent.getId());

        SpawnRequest saved = spawnRepository.save(request);
        auditService.log(actor, "APPROVE_SPAWN", "spawn_request", id,
            String.format("{\"approvedBy\":\"%s\",\"agentId\":\"%s\"}", approvedBy, agent.getId()));
        return saved;
    }

    @Transactional
    public Agent activateAgent(SpawnRequest request, String actor) {
        String agentId = UUID.randomUUID().toString();
        String name = request.getCustomRole() != null ? request.getCustomRole()
            : (request.getPurpose() != null ? request.getPurpose() : "agent-" + agentId.substring(0, 8));

        Integer depth = 0;
        if (request.getRequestedByHumanId() != null) {
            depth = 1;
        } else if (request.getRequesterId() != null) {
            Optional<Agent> parentOpt = agentRepository.findById(request.getRequesterId());
            if (parentOpt.isPresent()) {
                depth = parentOpt.get().getLineageDepth() != null ? parentOpt.get().getLineageDepth() + 1 : 1;
            }
        }

        if (depth >= depthCap) {
            throw new IllegalStateException("Spawn depth cap reached: depth=" + depth + " cap=" + depthCap);
        }

        Agent agent = new Agent();
        agent.setId(agentId);
        agent.setName(name);
        // SPW-046: Persistent hire ownership derives from the gate's accepting human at activation,
        // not from the requester. Use requestedByHumanId when explicitly provided (approval-gated path);
        // otherwise fall back to requesterId for direct activation.
        agent.setOwnerHumanId(request.getRequestedByHumanId() != null
            ? request.getRequestedByHumanId()
            : request.getRequesterId());
        agent.setAgentClass(request.getSpawnClass());
        agent.setSpawnedBy(request.getRequesterId());
        agent.setLineageDepth(depth);
        agent.setTemplateId(request.getTemplateId());
        agent.setTemplateVersion(resolveTemplateVersion(request.getTemplateId()));
        agent.setBudgetCap(request.getBudgetCap());
        agent.setStatus("active");

        Agent saved = agentRepository.save(agent);
        auditService.log(actor, "SPAWN_AGENT", "agent", agentId,
            String.format("{\"requestId\":\"%s\",\"class\":\"%s\"}", request.getId(), request.getSpawnClass()));
        return saved;
    }

    private String resolveTemplateVersion(String templateId) {
        if (templateId == null || templateId.isBlank()) return null;
        return templateRepository.findById(templateId)
            .map(t -> t.getVersion() != null ? "v" + t.getVersion() : "v1")
            .orElse("v1");
    }

    @Transactional
    public SpawnRequest deny(String id, String actor) {
        SpawnRequest request = spawnRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Spawn request not found: " + id));

        request.setStatus("denied");
        SpawnRequest saved = spawnRepository.save(request);
        auditService.log(actor, "DENY_SPAWN", "spawn_request", id, null);
        return saved;
    }

    @Transactional
    public SpawnRequest archive(String id, String actor) {
        SpawnRequest request = spawnRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Spawn request not found: " + id));

        request.setStatus("archived");
        SpawnRequest saved = spawnRepository.save(request);
        auditService.log(actor, "ARCHIVE_SPAWN", "spawn_request", id, null);
        return saved;
    }

    public Map<String, Object> getStats() {
        long requested = spawnRepository.countByStatus("requested");
        long approved = spawnRepository.countByStatus("approved");
        long archived = spawnRepository.countByStatus("archived");
        return Map.of("requested", requested, "approved", approved, "archived", archived);
    }

    /**
     * Check if spend ceiling is breached per SPW-060.
     * Delegates to GovernanceService to avoid duplication.
     */
    public boolean isSpendHaltTripped() {
        return governanceService.isSpendHaltTripped();
    }
}
