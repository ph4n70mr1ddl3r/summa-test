package com.summa.controller;

import com.summa.service.TriggerService;
import com.summa.model.Trigger;
import com.summa.service.AuditService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.service.AgentService;
import com.summa.service.WorkspaceService;
import com.summa.service.MemberService;
import com.summa.constants.Defaults;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/triggers")
public class TriggerController {
    private final TriggerService triggerService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final AgentService agentService;
    private final WorkspaceService workspaceService;
    private final MemberService memberService;

    public TriggerController(TriggerService triggerService, AuditService auditService, WriteGate writeGate,
                              AgentService agentService, WorkspaceService workspaceService, MemberService memberService) {
        this.triggerService = triggerService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.agentService = agentService;
        this.workspaceService = workspaceService;
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<?> listTriggers(
            @RequestParam(required = false) String agentId) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        if (agentId != null) {
            return ResponseEntity.ok(triggerService.findByAgent(agentId));
        }
        return ResponseEntity.ok(triggerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTrigger(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        Optional<Trigger> entOpt = triggerService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Trigger not found: " + id);
    }

    @PostMapping
    public ResponseEntity<?> createTrigger(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            String name = body.get("name");
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("name is required");
            }
            String kind = body.get("kind");
            if (kind == null || kind.isBlank()) {
                throw new IllegalArgumentException("kind is required");
            }
            String expression = body.get("expression");
            if (expression == null || expression.isBlank()) {
                throw new IllegalArgumentException("expression is required");
            }
            String agentId = body.get("agentId");
            if (agentId == null || agentId.isBlank()) {
                throw new IllegalArgumentException("agentId is required");
            }
            if (agentService.findById(agentId).isEmpty()) {
                throw new IllegalArgumentException("Agent not found: " + agentId);
            }
            String workspaceId = body.get("workspaceId");
            if (workspaceId != null && !workspaceId.isBlank() && workspaceService.findById(workspaceId).isEmpty()) {
                throw new IllegalArgumentException("Workspace not found: " + workspaceId);
            }
            Trigger trigger = triggerService.create(
                name,
                kind,
                expression,
                agentId,
                body.get("workspaceId"),
                body.get("criticality"),
                body.get("config"),
                actor
            );
            return ResponseEntity.ok(trigger);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<?> pause(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Trigger trigger = triggerService.pause(id, actor);
            return ResponseEntity.ok(trigger);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resume(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Trigger trigger = triggerService.resume(id, actor);
            return ResponseEntity.ok(trigger);
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
            Trigger trigger = triggerService.archive(id, actor);
            return ResponseEntity.ok(trigger);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Admin access required to view trigger stats");
        }
        return ResponseEntity.ok(triggerService.getStats());
    }
}
