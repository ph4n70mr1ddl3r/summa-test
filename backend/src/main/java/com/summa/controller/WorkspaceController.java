package com.summa.controller;

import com.summa.service.WorkspaceService;
import com.summa.model.Workspace;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import com.summa.service.MemberService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workspaces")
public class WorkspaceController {
    private final WorkspaceService workspaceService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final MemberService memberService;

    public WorkspaceController(WorkspaceService workspaceService, AuditService auditService, WriteGate writeGate,
                               MemberService memberService) {
        this.workspaceService = workspaceService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.memberService = memberService;
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
    public ResponseEntity<?> createWorkspace(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            if (body.get("name") == null || body.get("name").isBlank()) {
                throw new IllegalArgumentException("name is required");
            }
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
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "validation", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        } catch (IllegalStateException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/rebind")
    public ResponseEntity<?> rebind(@PathVariable String id,
                                     @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Workspace ws = workspaceService.rebind(id, body.get("targetNodeId"), actor);
            return ResponseEntity.ok(ws);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archive(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!memberService.isAdmin(actor)) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "admin_only", "Workspace archive requires admin role", actor);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "admin_only", "message", "Workspace archive requires admin role",
                            "audit_event_id", audit.getId()));
        }
        try {
            Workspace ws = workspaceService.archive(id, actor);
            return ResponseEntity.ok(ws);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }
}
