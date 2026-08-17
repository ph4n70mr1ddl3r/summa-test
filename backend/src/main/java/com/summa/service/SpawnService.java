package com.summa.service;

import com.summa.repository.SpawnRequestRepository;
import com.summa.model.SpawnRequest;
import com.summa.repository.RoleTemplateRepository;
import com.summa.model.RoleTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class SpawnService {
    private final SpawnRequestRepository spawnRepository;
    private final AuditService auditService;
    private final GovernanceService governanceService;
    private final RoleTemplateRepository templateRepository;

    public SpawnService(SpawnRequestRepository spawnRepository, AuditService auditService,
                          GovernanceService governanceService, RoleTemplateRepository templateRepository) {
        this.spawnRepository = spawnRepository;
        this.auditService = auditService;
        this.governanceService = governanceService;
        this.templateRepository = templateRepository;
    }

    public SpawnRequest create(String requesterId, String templateId, String customRole,
                                 String spawnClass, String purpose, String workspaceBindings,
                                 String scopeCeiling, Double budgetCap, Integer ttlHours,
                                 String requestedByHumanId, String actor) {
        // SPW-060: Check spend halt
        if (governanceService.isSpendHaltTripped()) {
            throw new IllegalStateException("Spend halt is active - spawns are blocked");
        }

        // SPW-020/021: Validate template if named
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
            // SPW-021: Class must match — persistent hire needs persistent template,
            // ephemeral needs ephemeral-subagent template
            String requiredClass = "ephemeral".equals(spawnClass) ? "ephemeral-subagent" : "persistent";
            if (!requiredClass.equals(template.getAgentClass())) {
                throw new IllegalStateException("Template class mismatch: request class="
                    + spawnClass + " but template class=" + template.getAgentClass());
            }
        }

        SpawnRequest request = new SpawnRequest();
        request.setId(java.util.UUID.randomUUID().toString());
        request.setRequesterId(requesterId);
        request.setTemplateId(templateId);
        request.setCustomRole(customRole);
        request.setSpawnClass(spawnClass != null ? spawnClass : "ephemeral");
        request.setPurpose(purpose);
        request.setWorkspaceBindings(workspaceBindings != null ? workspaceBindings : "[]");
        request.setScopeCeiling(scopeCeiling != null ? scopeCeiling : "{}");
        request.setBudgetCap(budgetCap);
        request.setTtlHours(ttlHours);
        request.setRequestedByHumanId(requestedByHumanId);
        request.setStatus("requested");

        SpawnRequest saved = spawnRepository.save(request);
        auditService.log(actor, "CREATE_SPAWN", "spawn_request", saved.getId(),
            String.format("{\"class\":\"%s\",\"templateId\":\"%s\"}", spawnClass, templateId));
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

        // SPW-062: Check spend halt — accept under halt is audit-only
        if (governanceService.isSpendHaltTripped()) {
            request.setStatus("archived");
            SpawnRequest saved = spawnRepository.save(request);
            auditService.log(actor, "AUDIT_ONLY_SPAWN_APPROVE", "spawn_request", id,
                "Rejected: spend halt active");
            return saved;
        }

        request.setStatus("approved");
        request.setApprovedBy(approvedBy);
        request.setApprovedAt(Instant.now());

        SpawnRequest saved = spawnRepository.save(request);
        auditService.log(actor, "APPROVE_SPAWN", "spawn_request", id,
            String.format("{\"approvedBy\":\"%s\"}", approvedBy));
        return saved;
    }

    @Transactional
    public SpawnRequest deny(String id, String actor) {
        SpawnRequest request = spawnRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Spawn request not found: " + id));

        request.setStatus("archived");
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

    public java.util.Map<String, Object> getStats() {
        long requested = spawnRepository.countByStatus("requested");
        long approved = spawnRepository.countByStatus("approved");
        long archived = spawnRepository.countByStatus("archived");
        return java.util.Map.of("requested", requested, "approved", approved, "archived", archived);
    }

    /**
     * Check if spend ceiling is breached per SPW-060.
     * Delegates to GovernanceService to avoid duplication.
     */
    public boolean isSpendHaltTripped() {
        return governanceService.isSpendHaltTripped();
    }
}
