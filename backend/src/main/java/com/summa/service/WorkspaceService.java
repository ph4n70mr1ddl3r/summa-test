package com.summa.service;

import com.summa.repository.NodeRepository;
import com.summa.model.Node;
import com.summa.repository.WorkspaceRepository;
import com.summa.repository.DnaDomainRepository;
import com.summa.model.Workspace;
import com.summa.repository.InitiativeRepository;
import com.summa.repository.TriggerRepository;
import com.summa.model.Trigger;
import com.summa.repository.SpawnRequestRepository;
import com.summa.model.SpawnRequest;
import com.summa.repository.RunRepository;
import com.summa.model.Run;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.summa.constants.Defaults;
import com.summa.exception.EntityNotFoundException;
import com.summa.util.JsonHelpers;

@Service
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final DnaDomainRepository domainRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final InitiativeRepository initiativeRepository;
    private final TriggerRepository triggerRepository;
    private final SpawnRequestRepository spawnRequestRepository;
    private final NodeRepository nodeRepository;
    private final RunRepository runRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository, DnaDomainRepository domainRepository,
                            AuditService auditService, ObjectMapper objectMapper,
                            InitiativeRepository initiativeRepository,
                            TriggerRepository triggerRepository,
                            SpawnRequestRepository spawnRequestRepository,
                            NodeRepository nodeRepository,
                            RunRepository runRepository) {
        this.workspaceRepository = workspaceRepository;
        this.domainRepository = domainRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.initiativeRepository = initiativeRepository;
        this.triggerRepository = triggerRepository;
        this.spawnRequestRepository = spawnRequestRepository;
        this.nodeRepository = nodeRepository;
        this.runRepository = runRepository;
    }

    @Transactional
    public Workspace create(String id, String name, String kind, String domainIds,
                              String initiativeIds, String nodeId, String participants, String actor) {
        // Validate referenced domains exist
        if (domainIds != null && !domainIds.isBlank() && !domainIds.equals("[]")) {
            try {
                List<String> domainIdList = objectMapper.readValue(domainIds, new TypeReference<List<String>>() {});
                for (String domId : domainIdList) {
                    domainRepository.findById(domId).orElseThrow(
                        () -> new EntityNotFoundException("Domain not found: " + domId));
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid domainIds format: " + e.getMessage());
            }
        }

        Workspace ws = new Workspace();
        ws.setId(id);
        ws.setName(name);
        ws.setKind(kind != null ? kind : "project");
        ws.setDomainIds(domainIds != null ? domainIds : "[]");
        ws.setInitiativeIds(initiativeIds != null ? initiativeIds : "[]");
        ws.setNodeId(nodeId);
        ws.setParticipants(participants != null ? participants : "[]");

        Workspace saved = workspaceRepository.save(ws);
        auditService.log(actor, "CREATE_WORKSPACE", "workspace", id,
            String.format("{\"name\":%s,\"kind\":%s}", JsonHelpers.jsonString(name), JsonHelpers.jsonString(ws.getKind())));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Workspace> findById(String id) {
        return workspaceRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Workspace> findAllActive() {
        return workspaceRepository.findByArchivedAtIsNull();
    }

    @Transactional(readOnly = true)
    public List<Workspace> findAllActive(int limit) {
        return workspaceRepository.findByArchivedAtIsNullOrdered(limit);
    }

    @Transactional(readOnly = true)
    public List<Workspace> findByNode(String nodeId) {
        return workspaceRepository.findByNodeId(nodeId);
    }

    @Transactional
    public Workspace rebind(String id, String targetNodeId, String actor) {
        Workspace ws = workspaceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Workspace not found: " + id));

        // API-043: refuse a target node lacking required capabilities (ARC-012)
        if (targetNodeId != null && !targetNodeId.isBlank()) {
            Optional<Node> nodeOpt = nodeRepository.findById(targetNodeId);
            if (nodeOpt.isEmpty()) {
                throw new EntityNotFoundException("Target node not found: " + targetNodeId);
            }
            Node targetNode = nodeOpt.get();
            if (targetNode.isRevoked()) {
                throw new IllegalStateException("Target node is revoked: " + targetNodeId);
            }
            // Verify the node has at least minimal capabilities (non-empty JSON object)
            if (targetNode.getCapabilities() == null || targetNode.getCapabilities().isBlank()
                    || targetNode.getCapabilities().equals("{}")) {
                throw new IllegalStateException("Target node lacks required capabilities (ARC-012): " + targetNodeId);
            }
        }

        ws.setNodeId(targetNodeId);
        Workspace saved = workspaceRepository.save(ws);
        auditService.log(actor, "REBIND_WORKSPACE", "workspace", id,
            String.format("{\"targetNodeId\":%s}", JsonHelpers.jsonString(targetNodeId)));
        return saved;
    }

    /**
     * CLC-040: Workspace archival walk.
     * - Initiative bindings drop (goal slice re-derives)
     * - Domain reader sets re-derive
     * - Node claim dies with the row
     * - New spawn bindings are refused
     * - Pending spawn requests binding to it archive with pins drained
     * - In-flight runs complete onto the archived slice
     * - Bound triggers and playbook schedules re-point or disable
     * - Project memory archives inert
     */
    @Transactional
    public Workspace archive(String id, String actor) {
        Workspace ws = workspaceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Workspace not found: " + id));

        if (ws.isArchived()) {
            throw new IllegalStateException("Workspace is already archived");
        }

        // Drop initiative bindings — goal slice re-derives at once
        ws.setInitiativeIds("[]");

        // Kill the node claim — clear only this workspace's ref from the node's claim JSON
        String previousNodeId = ws.getNodeId();
        ws.setNodeId(null);
        ws.setClaimEpoch(0);
        ws.setLeaseExpiresAt(null);
        if (previousNodeId != null) {
            nodeRepository.findById(previousNodeId).ifPresent(node -> {
                String claim = node.getClaim();
                if (claim != null && !claim.isBlank()) {
                    JsonNode claimNode = null;
                    try {
                        claimNode = objectMapper.readTree(claim);
                        if (claimNode.isArray()) {
                            List<String> remaining = new ArrayList<>();
                            for (JsonNode c : claimNode) {
                                if (c.isObject() && c.has("workspaceId")) {
                                    String wsId = c.get("workspaceId").asText();
                                    if (!wsId.equals(id) && !remaining.contains(wsId)) {
                                        remaining.add(wsId);
                                    }
                                } else if (c.isTextual() && !c.asText().equals(id)) {
                                    remaining.add(c.asText());
                                }
                            }
                            node.setClaim(objectMapper.writeValueAsString(remaining));
                        } else if (claimNode.isObject()) {
                            // Legacy single-object claim format — clear entirely
                            node.setClaim(null);
                        }
                        nodeRepository.save(node);
                    } catch (Exception e) {
                        // On parse error, remove only this workspace's entry rather than nullifying the entire claim.
                        // This prevents data loss for other workspaces sharing the same node.
                        if (claimNode != null) {
                            try {
                                if (claimNode.isArray()) {
                                    List<String> remaining = new ArrayList<>();
                                    for (JsonNode c : claimNode) {
                                        if (c.isObject() && c.has("workspaceId")) {
                                            String wsId = c.get("workspaceId").asText();
                                            if (!wsId.equals(id) && !remaining.contains(wsId)) {
                                                remaining.add(wsId);
                                            }
                                        } else if (c.isTextual() && !c.asText().equals(id)) {
                                            remaining.add(c.asText());
                                        }
                                    }
                                    node.setClaim(objectMapper.writeValueAsString(remaining));
                                } else if (claimNode.isObject()) {
                                    // Legacy single-object claim format — clear entirely
                                    node.setClaim(null);
                                }
                            } catch (Exception parseError) {
                                auditService.logSystem("ARCHIVE_CLEAR_NODE_CLAIM_PARSE_FAIL", "node", previousNodeId,
                                    JsonHelpers.toJson(Map.of("workspaceId", id, "error", parseError.getMessage()), objectMapper));
                            }
                        }
                        nodeRepository.save(node);
                    }
                }
                auditService.log(actor, "ARCHIVE_CLEAR_NODE_CLAIM", "node", previousNodeId,
                    String.format("{\"workspaceId\":%s,\"reason\":\"workspace_archived\"}", JsonHelpers.jsonString(id)));
            });
        }

        // Disable bound triggers and playbooks
        List<Trigger> boundTriggers = triggerRepository.findByWorkspaceId(id);
        for (Trigger t : boundTriggers) {
            if ("active".equals(t.getStatus())) {
                t.setStatus("paused");
                triggerRepository.save(t);
                auditService.log(actor, "ARCHIVE_PAUSE_TRIGGER", "trigger", t.getId(),
                    String.format("{\"workspaceId\":%s,\"reason\":\"workspace_archived\"}", JsonHelpers.jsonString(id)));
            }
        }

        // CLC-040: Archive pending spawn requests binding to this workspace
        List<SpawnRequest> pendingSpawns = spawnRequestRepository.findPendingByWorkspaceBinding(
            SpawnRequestRepository.escapeLike(id));
        for (SpawnRequest sr : pendingSpawns) {
            sr.setStatus("archived");
            spawnRequestRepository.save(sr);
            auditService.log(actor, "ARCHIVE_PENDING_SPAWN", "spawn_request", sr.getId(),
                String.format("{\"workspaceId\":%s,\"reason\":\"workspace_archived\"}", JsonHelpers.jsonString(id)));
        }

        // CLC-041: Cancel queued/running runs binding to this workspace
        List<Run> activeRuns = runRepository.findByWorkspaceIdAndStatusIn(id, List.of("queued", "running"));
        for (Run run : activeRuns) {
            run.setStatus("cancelled");
            run.setCompletedAt(Instant.now());
            runRepository.save(run);
            auditService.log(actor, "ARCHIVE_CANCEL_RUN", "run", run.getId(),
                String.format("{\"workspaceId\":%s,\"reason\":\"workspace_archived\"}", JsonHelpers.jsonString(id)));
        }

        ws.setArchivedAt(Instant.now());
        Workspace saved = workspaceRepository.save(ws);
        auditService.log(actor, "ARCHIVE_WORKSPACE", "workspace", id, null);
        return saved;
    }
}
