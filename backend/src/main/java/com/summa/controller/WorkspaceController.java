package com.summa.controller;

import com.summa.security.RbacAuthorizationFilter;
import com.summa.security.WriteGate;
import com.summa.service.AuditService;
import com.summa.service.MemberService;
import com.summa.service.WorkspaceService;
import com.summa.model.Workspace;
import com.summa.constants.Defaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;
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
    public ResponseEntity<?> listWorkspaces(
            @RequestParam(defaultValue = "50") int limit) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        int cappedLimit = Math.min(Math.max(limit, 1), Defaults.MAX_LIST_LIMIT);
        return ResponseEntity.ok(workspaceService.findAllActive(cappedLimit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getWorkspace(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        Optional<Workspace> entOpt = workspaceService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Workspace not found: " + id);
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
            String kind = body.get("kind");
            if (kind != null && !"project".equals(kind) && !"personal".equals(kind) && !"system".equals(kind)) {
                throw new IllegalArgumentException("kind must be 'project', 'personal', or 'system'");
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
                body.get("participants"),
                actor
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
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archive(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Workspace ws = workspaceService.archive(id, actor);
            return ResponseEntity.ok(ws);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }
}
