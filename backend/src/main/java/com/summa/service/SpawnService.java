package com.summa.service;

import com.summa.repository.SpawnRequestRepository;
import com.summa.model.SpawnRequest;
import com.summa.repository.RoleTemplateRepository;
import com.summa.model.RoleTemplate;
import com.summa.repository.AgentRepository;
import com.summa.model.Agent;
import com.summa.repository.WorkspaceRepository;
import com.summa.model.Workspace;
import com.summa.repository.DnaDomainRepository;
import com.summa.model.DnaDomain;
import com.summa.repository.InitiativeRepository;
import com.summa.model.Initiative;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SpawnService {
    private final SpawnRequestRepository spawnRepository;
    private final AuditService auditService;
    private final GovernanceService governanceService;
    private final RoleTemplateRepository templateRepository;
    private final AgentRepository agentRepository;
    private final MemberService memberService;
    private final WorkspaceRepository workspaceRepository;
    private final DnaDomainRepository domainRepository;
    private final AskService askService;
    private final SpendLedgerService spendLedgerService;
    private final InitiativeRepository initiativeRepository;
    private final ObjectMapper objectMapper;

    @Value("${summa.spawn.depth-cap:2}")
    private int depthCap;

    public SpawnService(SpawnRequestRepository spawnRepository, AuditService auditService,
                          GovernanceService governanceService, RoleTemplateRepository templateRepository,
                          AgentRepository agentRepository, MemberService memberService,
                          WorkspaceRepository workspaceRepository,
                          DnaDomainRepository domainRepository,
                          AskService askService, SpendLedgerService spendLedgerService,
                          InitiativeRepository initiativeRepository,
                          ObjectMapper objectMapper) {
        this.spawnRepository = spawnRepository;
        this.auditService = auditService;
        this.governanceService = governanceService;
        this.templateRepository = templateRepository;
        this.agentRepository = agentRepository;
        this.memberService = memberService;
        this.workspaceRepository = workspaceRepository;
        this.domainRepository = domainRepository;
        this.askService = askService;
        this.spendLedgerService = spendLedgerService;
        this.initiativeRepository = initiativeRepository;
        this.objectMapper = objectMapper;
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

        // SPW-010: Ephemeral requester refused a persistent-hire request at write
        if ("persistent".equals(effectiveSpawnClass)) {
            if (requesterOpt.isPresent() && "ephemeral".equals(requesterOpt.get().getAgentClass())) {
                throw new IllegalStateException("Ephemeral agents cannot request persistent hires");
            }
        }

        // INT-080: Only active initiatives launch spawns — verify workspace bindings reference active initiatives
        if (workspaceBindings != null && !workspaceBindings.isBlank() && !workspaceBindings.equals("[]")) {
            try {
                JsonNode bindings = objectMapper.readTree(workspaceBindings);
                if (bindings.isArray()) {
                    for (JsonNode binding : bindings) {
                        String wsId = binding.asText();
                        Optional<com.summa.model.Workspace> wsOpt = workspaceRepository.findById(wsId);
                        if (wsOpt.isPresent()) {
                            Workspace ws = wsOpt.get();
                            if (ws.getInitiativeIds() != null && !ws.getInitiativeIds().isBlank()
                                    && !ws.getInitiativeIds().equals("[]")) {
                                JsonNode initIds = objectMapper.readTree(ws.getInitiativeIds());
                                if (initIds.isArray()) {
                                    for (JsonNode initIdNode : initIds) {
                                        String initId = initIdNode.asText();
                                        Optional<Initiative> initOpt = initiativeRepository.findById(initId);
                                        if (initOpt.isPresent() && !"active".equals(initOpt.get().getStatus())) {
                                            throw new IllegalStateException(
                                                "Workspace binds to non-active initiative: " + initId
                                                    + " (status: " + initOpt.get().getStatus() + ")");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                auditService.logSystem("SPAWN_PARSE_BINDINGS_FAIL", "spawn_request", UUID.randomUUID().toString(),
                    String.format("{\"error\":\"%s\"}", e.getMessage()));
            }
        }

        // SPW-030: Scope delegation — child's scopes must be ⊆ parent's scopes
        if (scopeCeiling != null && !scopeCeiling.isBlank() && !scopeCeiling.equals("{}")) {
            validateScopeCeiling(requesterId, scopeCeiling);
        }

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

        // SPW-040: Determine the approval gate for persistent hires
        // Gate routes to the owner of the primary domain of the hire's primary workspace
        String gateTarget = null;
        if ("persistent".equals(effectiveSpawnClass) && workspaceBindings != null && !workspaceBindings.isBlank()) {
            try {
                JsonNode bindings = objectMapper.readTree(workspaceBindings);
                if (bindings.isArray() && bindings.size() > 0) {
                    // Primary workspace is the first-bound entry
                    String primaryWorkspaceId = bindings.get(0).asText();
                    Optional<Workspace> wsOpt = workspaceRepository.findById(primaryWorkspaceId);
                    if (wsOpt.isPresent()) {
                        Workspace ws = wsOpt.get();
                        String domainIdsStr = ws.getDomainIds();
                        if (domainIdsStr != null && !domainIdsStr.isBlank() && !domainIdsStr.equals("[]")) {
                            JsonNode domIds = objectMapper.readTree(domainIdsStr);
                            if (domIds.isArray() && domIds.size() > 0) {
                                // DAT-090: first entry is primary domain
                                String primaryDomainId = domIds.get(0).asText();
                                Optional<DnaDomain> domOpt = domainRepository.findById(primaryDomainId);
                                if (domOpt.isPresent()) {
                                    gateTarget = domOpt.get().getOwnerHumanId();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                auditService.logSystem("SPAWN_PARSE_BINDINGS_FAIL", "spawn_request", UUID.randomUUID().toString(),
                    String.format("{\"error\":\"%s\"}", e.getMessage()));
            }
        }

        // SPW-040 fallback: multi-domain routes to primary domain owner; domainless/primary empty → admin
        if (gateTarget == null || gateTarget.isBlank()) {
            gateTarget = OffboardingWalkService.ADMIN_BROADCAST;
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
        request.setGateTarget(gateTarget);

        SpawnRequest saved = spawnRepository.save(request);
        auditService.log(actor, "CREATE_SPAWN", "spawn_request", saved.getId(),
            String.format("{\"class\":\"%s\",\"templateId\":\"%s\",\"gateTarget\":\"%s\"}", effectiveSpawnClass, templateId, gateTarget));
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
        Agent agent = activateAgent(request, actor, approvedBy);
        request.setAgentId(agent.getId());

        SpawnRequest saved = spawnRepository.save(request);
        auditService.log(actor, "APPROVE_SPAWN", "spawn_request", id,
            String.format("{\"approvedBy\":\"%s\",\"agentId\":\"%s\"}", approvedBy, agent.getId()));
        return saved;
    }

    @Transactional
    public Agent activateAgent(SpawnRequest request, String actor, String approvedByHumanId) {
        String agentId = UUID.randomUUID().toString();
        String name = request.getCustomRole() != null ? request.getCustomRole()
            : (request.getPurpose() != null ? request.getPurpose() : "agent-" + agentId.substring(0, 8));

        Integer depth = 0;
        if (request.getRequestedByHumanId() != null) {
            // Direct human request: human is root, agent depth = 0
            depth = 0;
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
        // SPW-046: Ownership derives from the gate's accepting human at activation,
        // not from the requester. Fall back to requestedByHumanId, then requester.
        String ownerHumanId = approvedByHumanId;
        if (ownerHumanId == null || ownerHumanId.isBlank()) {
            ownerHumanId = request.getRequestedByHumanId();
        }
        if (ownerHumanId == null || ownerHumanId.isBlank()) {
            ownerHumanId = request.getRequesterId();
            // Validate it is a human ID, not an agent ID — owner_human_id is a FK to humans(id)
            Optional<com.summa.model.Human> maybeHuman = memberService.findHuman(ownerHumanId);
            if (maybeHuman.isEmpty()) {
                throw new IllegalStateException("Cannot activate spawn without a human owner: no approvedBy, requestedByHumanId, and requester is not a human");
            }
        }
        agent.setOwnerHumanId(ownerHumanId);
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

    /**
     * SPW-030: Validate that the requested scope ceiling is a subset of the parent agent's scopes.
     * Returns true if valid, throws if the child's scopes exceed the parent's.
     */
    private void validateScopeCeiling(String requesterId, String scopeCeiling) {
        try {
            Optional<Agent> parentOpt = agentRepository.findById(requesterId);
            if (parentOpt.isEmpty()) return;
            Agent parent = parentOpt.get();
            if (parent.getTemplateId() == null) return; // No template = no scope constraints

            Optional<RoleTemplate> templateOpt = templateRepository.findById(parent.getTemplateId());
            if (templateOpt.isEmpty()) return;

            RoleTemplate template = templateOpt.get();
            String parentScopes = template.getDefaultScopes();
            if (parentScopes == null || parentScopes.isBlank() || parentScopes.equals("{}")) return;

            JsonNode parentJson = objectMapper.readTree(parentScopes);
            JsonNode childJson = objectMapper.readTree(scopeCeiling);

            // Validate: every key in child must exist in parent with equal or narrower value
            java.util.Iterator<String> childKeys = childJson.fieldNames();
            while (childKeys.hasNext()) {
                String key = childKeys.next();
                if (!parentJson.has(key)) {
                    throw new IllegalStateException(
                        "Scope ceiling violation: child requests scope '" + key + "' not granted to parent");
                }
                // Check value containment — string values must match exactly for simplicity
                String parentVal = parentJson.get(key).asText();
                String childVal = childJson.get(key).asText();
                if (!parentVal.equals(childVal)) {
                    throw new IllegalStateException(
                        "Scope ceiling violation: child requests '" + key + "=" + childVal
                            + "' but parent only has '" + key + "=" + parentVal + "'");
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            auditService.logSystem("SPAWN_SCOPE_VALIDATE_FAIL", "spawn_request", UUID.randomUUID().toString(),
                String.format("{\"requesterId\":\"%s\",\"error\":\"%s\"}", requesterId, e.getMessage()));
        }
    }

    public Map<String, Object> getStats() {
        long requested = spawnRepository.countByStatus("requested");
        long approved = spawnRepository.countByStatus("approved");
        long archived = spawnRepository.countByStatus("archived");
        return Map.of("requested", requested, "approved", approved, "archived", archived);
    }
}
