package com.summa.service;

import com.summa.repository.WorkspaceRepository;
import com.summa.repository.DnaDomainRepository;
import com.summa.model.Workspace;
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

    public WorkspaceService(WorkspaceRepository workspaceRepository, DnaDomainRepository domainRepository,
                            AuditService auditService, ObjectMapper objectMapper) {
        this.workspaceRepository = workspaceRepository;
        this.domainRepository = domainRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
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
    public Workspace archive(String id, String actor) {
        Workspace ws = workspaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + id));

        ws.setArchivedAt(Instant.now());
        Workspace saved = workspaceRepository.save(ws);
        auditService.log(actor, "ARCHIVE_WORKSPACE", "workspace", id, null);
        return saved;
    }
}
