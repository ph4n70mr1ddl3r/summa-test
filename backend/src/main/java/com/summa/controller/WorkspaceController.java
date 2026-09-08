package com.summa.controller;

import com.summa.security.RbacAuthorizationFilter;
import com.summa.security.WriteGate;
import com.summa.service.AuditService;
import com.summa.service.MemberService;
import com.summa.service.WorkspaceService;
import com.summa.model.Workspace;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
            // Security: reject client-supplied IDs — always generate server-side
            String generatedId = UUID.randomUUID().toString();
            Workspace ws = workspaceService.create(
                generatedId,
                body.get("name"),
                body.get("kind"),
                body.get("domainIds"),
                body.get("initiativeIds"),
                body.get("nodeId"),
                body.get("participants")
            );
            return ResponseEntity.ok(ws);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
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
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archive(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, "Workspace archive requires admin role");
        }
        try {
            Workspace ws = workspaceService.archive(id, actor);
            return ResponseEntity.ok(ws);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }
}
