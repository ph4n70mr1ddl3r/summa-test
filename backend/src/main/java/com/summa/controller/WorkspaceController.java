package com.summa.controller;

import com.summa.service.WorkspaceService;
import com.summa.model.Workspace;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workspaces")
public class WorkspaceController {
    private final WorkspaceService workspaceService;
    private final AuditService auditService;

    public WorkspaceController(WorkspaceService workspaceService, AuditService auditService) {
        this.workspaceService = workspaceService;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<Workspace>> listWorkspaces() {
        return ResponseEntity.ok(workspaceService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getWorkspace(@PathVariable String id) {
        return workspaceService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createWorkspace(@RequestBody Map<String, String> body,
                                              @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Workspace ws = workspaceService.create(
                body.get("id"),
                body.get("name"),
                body.get("kind"),
                body.get("domainIds"),
                body.get("initiativeIds"),
                body.get("nodeId"),
                body.get("participants")
            );
            return ResponseEntity.ok(ws);
        } catch (Exception e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.badRequest().body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/rebind")
    public ResponseEntity<?> rebind(@PathVariable String id,
                                     @RequestBody Map<String, String> body,
                                     @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Workspace ws = workspaceService.rebind(id, body.get("targetNodeId"), actor);
            return ResponseEntity.ok(ws);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archive(@PathVariable String id,
                                      @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Workspace ws = workspaceService.archive(id, actor);
            return ResponseEntity.ok(ws);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
