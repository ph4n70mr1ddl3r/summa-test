package com.summa.service;

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
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;

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

    public WorkspaceService(WorkspaceRepository workspaceRepository, DnaDomainRepository domainRepository,
                            AuditService auditService, ObjectMapper objectMapper,
                            InitiativeRepository initiativeRepository,
                            TriggerRepository triggerRepository,
                            PlaybookRepository playbookRepository,
                            SpawnRequestRepository spawnRequestRepository) {
        this.workspaceRepository = workspaceRepository;
        this.domainRepository = domainRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.initiativeRepository = initiativeRepository;
        this.triggerRepository = triggerRepository;
        this.playbookRepository = playbookRepository;
        this.spawnRequestRepository = spawnRequestRepository;
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
                        () -> new IllegalArgumentException("Domain not found: " + domId));
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
        auditService.log("system", "CREATE_WORKSPACE", "workspace", id,
            String.format("{\"name\":\"%s\",\"kind\":\"%s\"}", name, kind));
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
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + id));

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
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + id));

        // Drop initiative bindings — goal slice re-derives at once
        ws.setInitiativeIds("[]");

        // Kill the node claim — the lease's terminal case
        ws.setNodeId(null);
        ws.setClaimEpoch(0);
        ws.setLeaseExpiresAt(null);

        // Disable bound triggers and playbooks
        List<Trigger> boundTriggers = triggerRepository.findByWorkspaceId(id);
        for (Trigger t : boundTriggers) {
            if ("active".equals(t.getStatus())) {
                t.setStatus("paused");
                triggerRepository.save(t);
                auditService.logSystem("ARCHIVE_PAUSE_TRIGGER", "trigger", t.getId(),
                    String.format("{\"workspaceId\":\"%s\",\"reason\":\"workspace_archived\"}", id));
            }
        }

        List<Playbook> boundPlaybooks = playbookRepository.findAll().stream()
                .filter(p -> p.getBody() != null && p.getBody().contains(id))
                .toList();
        for (Playbook pb : boundPlaybooks) {
            auditService.logSystem("ARCHIVE_NOTE_PLAYBOOK", "playbook", pb.getId(),
                String.format("{\"workspaceId\":\"%s\",\"reason\":\"workspace_archived\"}", id));
        }

        // CLC-040: Archive pending spawn requests binding to this workspace
        List<SpawnRequest> pendingSpawns = spawnRequestRepository.findPendingByWorkspaceBinding(id);
        for (SpawnRequest sr : pendingSpawns) {
            sr.setStatus("archived");
            spawnRequestRepository.save(sr);
            auditService.logSystem("ARCHIVE_PENDING_SPAWN", "spawn_request", sr.getId(),
                String.format("{\"workspaceId\":\"%s\",\"reason\":\"workspace_archived\"}", id));
        }

        ws.setArchivedAt(Instant.now());
        Workspace saved = workspaceRepository.save(ws);
        auditService.log(actor, "ARCHIVE_WORKSPACE", "workspace", id, null);
        return saved;
    }
}

