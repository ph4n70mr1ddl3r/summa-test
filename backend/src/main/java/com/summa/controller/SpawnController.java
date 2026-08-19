package com.summa.controller;

import com.summa.service.SpawnService;
import com.summa.model.SpawnRequest;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
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

    public SpawnController(SpawnService spawnService, AuditService auditService, WriteGate writeGate) {
        this.spawnService = spawnService;
        this.auditService = auditService;
        this.writeGate = writeGate;
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
            SpawnRequest request = spawnService.create(
                requesterId,
                body.get("templateId"),
                body.get("customRole"),
                body.get("spawnClass") != null ? body.get("spawnClass") : "ephemeral",
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
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        } catch (IllegalStateException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            SpawnRequest request = spawnService.approve(id, actor, actor);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        } catch (IllegalStateException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/deny")
    public ResponseEntity<?> deny(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            SpawnRequest request = spawnService.deny(id, actor);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
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
