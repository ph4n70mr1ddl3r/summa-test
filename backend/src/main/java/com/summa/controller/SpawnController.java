package com.summa.controller;

import com.summa.service.SpawnService;
import com.summa.model.SpawnRequest;
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
@RequestMapping("/spawn")
public class SpawnController {
    private final SpawnService spawnService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final MemberService memberService;

    public SpawnController(SpawnService spawnService, AuditService auditService, WriteGate writeGate,
                           MemberService memberService) {
        this.spawnService = spawnService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<List<SpawnRequest>> listRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String requesterId) {
        if (status != null) {
            return ResponseEntity.ok(spawnService.findByStatus(status));
        }
        if (requesterId != null) {
            return ResponseEntity.ok(spawnService.findByRequester(requesterId));
        }
        return ResponseEntity.ok(spawnService.findByStatus("requested"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRequest(@PathVariable String id) {
        return spawnService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createRequest(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            String requesterId = body.get("requesterId");
            if (requesterId == null || requesterId.isBlank()) {
                throw new IllegalArgumentException("requesterId is required");
            }
            String rawClass = body.get("class");
            String effectiveClass = rawClass != null && !rawClass.isBlank() ? rawClass : "ephemeral";
            try {
                com.summa.enums.AgentClass.valueOf(effectiveClass.toUpperCase().replace("-", "_"));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid spawn class: " + effectiveClass
                    + ". Must be one of: persistent, ephemeral, ephemeral-subagent");
            }
            SpawnRequest request = spawnService.create(
                requesterId,
                body.get("templateId"),
                body.get("customRole"),
                effectiveClass,
                body.get("purpose"),
                body.get("workspaceBindings"),
                body.get("scopeCeiling"),
                body.containsKey("budgetCap") ? parseDoubleSafe(body.get("budgetCap")) : null,
                body.containsKey("ttlHours") ? parseIntSafe(body.get("ttlHours")) : null,
                body.get("requestedByHumanId"),
                actor
            );
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!memberService.isAdmin(actor)) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "admin_only", "Spawn approve requires admin role", actor);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "admin_only", "message", "Spawn approve requires admin role",
                            "audit_event_id", audit.getId()));
        }
        try {
            SpawnRequest request = spawnService.approve(id, actor, actor);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/deny")
    public ResponseEntity<?> deny(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!memberService.isAdmin(actor)) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "admin_only", "Spawn deny requires admin role", actor);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "admin_only", "message", "Spawn deny requires admin role",
                            "audit_event_id", audit.getId()));
        }
        try {
            SpawnRequest request = spawnService.deny(id, actor);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(spawnService.getStats());
    }

    private Double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid budgetCap: " + s); }
    }

    private Integer parseIntSafe(String s) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid ttlHours: " + s); }
    }
}
