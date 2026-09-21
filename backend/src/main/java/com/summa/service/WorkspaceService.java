package com.summa.service;

import com.summa.repository.NodeRepository;
import com.summa.model.Node;
import com.summa.repository.WorkspaceRepository;
import com.summa.repository.DnaDomainRepository;
import com.summa.model.Workspace;
import com.summa.repository.InitiativeRepository;
import com.summa.repository.TriggerRepository;
import com.summa.model.Initiative;
import com.summa.model.Trigger;
import com.summa.repository.PlaybookRepository;
import com.summa.model.Playbook;
import com.summa.repository.SpawnRequestRepository;
import com.summa.model.SpawnRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import com.summa.constants.Defaults;
import com.summa.exception.EntityNotFoundException;

@Service
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final DnaDomainRepository domainRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final InitiativeRepository initiativeRepository;
    private final TriggerRepository triggerRepository;
    private final PlaybookRepository playbookRepository;
    private final SpawnRequestRepository spawnRequestRepository;
    private final NodeRepository nodeRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository, DnaDomainRepository domainRepository,
                            AuditService auditService, ObjectMapper objectMapper,
                            InitiativeRepository initiativeRepository,
                            TriggerRepository triggerRepository,
                            PlaybookRepository playbookRepository,
                            SpawnRequestRepository spawnRequestRepository,
                            NodeRepository nodeRepository) {
        this.workspaceRepository = workspaceRepository;
        this.domainRepository = domainRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.initiativeRepository = initiativeRepository;
        this.triggerRepository = triggerRepository;
        this.playbookRepository = playbookRepository;
        this.spawnRequestRepository = spawnRequestRepository;
        this.nodeRepository = nodeRepository;
    }

    @Transactional
    public Workspace create(String id, String name, String kind, String domainIds,
                              String initiativeIds, String nodeId, String participants) {
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
        auditService.logSystem("CREATE_WORKSPACE", "workspace", id,
            String.format("{\"name\":\"%s\",\"kind\":\"%s\"}", name, ws.getKind()));
        return saved;
    }

    public Optional<Workspace> findById(String id) {
        return workspaceRepository.findById(id);
    }

    public List<Workspace> findAllActive() {
        return workspaceRepository.findByArchivedAtIsNull();
    }

    public List<Workspace> findByNode(String nodeId) {
        return workspaceRepository.findByNodeId(nodeId);
    }

    @Transactional
    public Workspace rebind(String id, String targetNodeId, String actor) {
        Workspace ws = workspaceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Workspace not found: " + id));

        ws.setNodeId(targetNodeId);
        Workspace saved = workspaceRepository.save(ws);
        auditService.log(actor, "REBIND_WORKSPACE", "workspace", id,
            String.format("{\"targetNodeId\":\"%s\"}", targetNodeId));
        return saved;
    }

    @Transactional
    public Workspace updateWorkspace(Workspace ws) {
        return workspaceRepository.save(ws);
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

        // Kill the node claim — clear the workspace-side ref and the node-side claim
        String previousNodeId = ws.getNodeId();
        ws.setNodeId(null);
        ws.setClaimEpoch(0);
        ws.setLeaseExpiresAt(null);
        if (previousNodeId != null) {
            nodeRepository.findById(previousNodeId).ifPresent(node -> {
                node.setClaim(null);
                nodeRepository.save(node);
                auditService.log(actor, "ARCHIVE_CLEAR_NODE_CLAIM", "node", previousNodeId,
                    String.format("{\"workspaceId\":\"%s\",\"reason\":\"workspace_archived\"}", id));
            });
        }

        // Disable bound triggers and playbooks
        List<Trigger> boundTriggers = triggerRepository.findByWorkspaceId(id);
        for (Trigger t : boundTriggers) {
            if ("active".equals(t.getStatus())) {
                t.setStatus("paused");
                triggerRepository.save(t);
                auditService.log(actor, "ARCHIVE_PAUSE_TRIGGER", "trigger", t.getId(),
                    String.format("{\"workspaceId\":\"%s\",\"reason\":\"workspace_archived\"}", id));
            }
        }

        List<Playbook> boundPlaybooks = new ArrayList<>();
        for (Playbook pb : playbookRepository.findAll()) {
            if (pb.getBody() != null && isWorkspaceReferencedInJson(pb.getBody(), id)) {
                boundPlaybooks.add(pb);
            }
        }
        for (Playbook pb : boundPlaybooks) {
            auditService.log(actor, "ARCHIVE_NOTE_PLAYBOOK", "playbook", pb.getId(),
                String.format("{\"workspaceId\":\"%s\",\"reason\":\"workspace_archived\"}", id));
        }

        // CLC-040: Archive pending spawn requests binding to this workspace
        List<SpawnRequest> pendingSpawns = spawnRequestRepository.findPendingByWorkspaceBinding(id);
        for (SpawnRequest sr : pendingSpawns) {
            sr.setStatus("archived");
            spawnRequestRepository.save(sr);
            auditService.log(actor, "ARCHIVE_PENDING_SPAWN", "spawn_request", sr.getId(),
                String.format("{\"workspaceId\":\"%s\",\"reason\":\"workspace_archived\"}", id));
        }

        ws.setArchivedAt(Instant.now());
        Workspace saved = workspaceRepository.save(ws);
        auditService.log(actor, "ARCHIVE_WORKSPACE", "workspace", id, null);
        return saved;
    }

    private boolean isWorkspaceReferencedInJson(String body, String workspaceId) {
        try {
            JsonNode root = objectMapper.readTree(body);
            return root.isArray()
                ? isWorkspaceInArray(root, workspaceId)
                : isWorkspaceInObject(root, workspaceId);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isWorkspaceInArray(JsonNode array, String workspaceId) {
        for (JsonNode element : array) {
            if (element.isTextual() && workspaceId.equals(element.asText())) {
                return true;
            }
            if (element.isObject() && isWorkspaceInObject(element, workspaceId)) {
                return true;
            }
            if (element.isArray() && isWorkspaceInArray(element, workspaceId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isWorkspaceInObject(JsonNode obj, String workspaceId) {
        if (obj.isObject()) {
            for (Iterator<Map.Entry<String, JsonNode>> it = obj.fields(); it.hasNext(); ) {
                JsonNode value = it.next().getValue();
                if (value.isTextual() && workspaceId.equals(value.asText())) {
                    return true;
                }
                if (value.isObject() && isWorkspaceInObject(value, workspaceId)) {
                    return true;
                }
                if (value.isArray() && isWorkspaceInArray(value, workspaceId)) {
                    return true;
                }
            }
        }
        return false;
    }
}

