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
import com.summa.util.JsonHelpers;
import com.summa.model.Agent;
import com.summa.util.Vocabulary;
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
            return ResponseEntity.ok(triggerService.findByAgent(JsonHelpers.stripIdPrefix(agentId)));
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
            // Validate kind against known trigger types
            if (!"schedule".equals(kind) && !"api".equals(kind) && !"event".equals(kind)) {
                throw new IllegalArgumentException("Invalid trigger kind: " + kind + ". Must be one of: schedule, api, event");
            }
            String expression = body.get("expression");
            if (expression == null || expression.isBlank()) {
                throw new IllegalArgumentException("expression is required");
            }
            // API-061: validation before constraint — triggers.criticality carries a CHECK.
            String criticality = Vocabulary.requireOneOf(body.get("criticality"), "criticality", "standard", "critical");
            String agentId = body.get("agentId");
            if (agentId == null || agentId.isBlank()) {
                throw new IllegalArgumentException("agentId is required");
            }
            agentId = JsonHelpers.stripIdPrefix(agentId);
            if (agentService.findById(agentId).isEmpty()) {
                throw new IllegalArgumentException("Agent not found: " + agentId);
            }
            // TRG-002: only the agent's owner or an admin may bind triggers to the agent
            if (!memberService.isAdmin(actor) && !isAgentOwner(agentId, actor)) {
                throw new IllegalStateException("Only the agent's owner or an admin may create triggers for this agent");
            }
            String workspaceId = body.get("workspaceId");
            if (workspaceId != null && !workspaceId.isBlank()) {
                workspaceId = JsonHelpers.stripIdPrefix(workspaceId);
                if (workspaceService.findById(workspaceId).isEmpty()) {
                    throw new IllegalArgumentException("Workspace not found: " + workspaceId);
                }
            }
            Trigger trigger = triggerService.create(
                name,
                kind,
                expression,
                agentId,
                workspaceId,
                criticality,
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
        String triggerId = JsonHelpers.stripIdPrefix(id);
        if (!mayControlTrigger(triggerId, actor)) {
            return ControllerResponses.gate(auditService, actor, "Only the agent's owner or an admin may pause this trigger");
        }
        try {
            Trigger trigger = triggerService.pause(triggerId, actor);
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
        String triggerId = JsonHelpers.stripIdPrefix(id);
        if (!mayControlTrigger(triggerId, actor)) {
            return ControllerResponses.gate(auditService, actor, "Only the agent's owner or an admin may resume this trigger");
        }
        try {
            Trigger trigger = triggerService.resume(triggerId, actor);
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
        String triggerId = JsonHelpers.stripIdPrefix(id);
        if (!mayControlTrigger(triggerId, actor)) {
            return ControllerResponses.gate(auditService, actor, "Only the agent's owner or an admin may archive this trigger");
        }
        try {
            Trigger trigger = triggerService.archive(triggerId, actor);
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
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Admin access required to view trigger stats");
        }
        return ResponseEntity.ok(triggerService.getStats());
    }

    private boolean isAgentOwner(String agentId, String actor) {
        Agent agent = agentService.findById(JsonHelpers.stripIdPrefix(agentId)).orElse(null);
        return agent != null
                && agent.getOwnerHumanId() != null
                && agent.getOwnerHumanId().equals(JsonHelpers.stripIdPrefix(actor));
    }

    private boolean mayControlTrigger(String triggerId, String actor) {
        Trigger trigger = triggerService.findById(triggerId).orElse(null);
        if (trigger == null) return false;
        return memberService.isAdmin(actor) || isAgentOwner(trigger.getAgentId(), actor);
    }
}
