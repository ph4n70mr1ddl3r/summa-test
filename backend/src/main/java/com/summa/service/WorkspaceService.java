package com.summa.service;

import com.summa.repository.WorkspaceRepository;
import com.summa.model.Workspace;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final AuditService auditService;

    public WorkspaceService(WorkspaceRepository workspaceRepository, AuditService auditService) {
        this.workspaceRepository = workspaceRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Workspace create(String id, String name, String kind, String domainIds,
                              String initiativeIds, String nodeId, String participants) {
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
    public Workspace archive(String id, String actor) {
        Workspace ws = workspaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + id));

        ws.setArchivedAt(Instant.now());
        Workspace saved = workspaceRepository.save(ws);
        auditService.log(actor, "ARCHIVE_WORKSPACE", "workspace", id, null);
        return saved;
    }
}
