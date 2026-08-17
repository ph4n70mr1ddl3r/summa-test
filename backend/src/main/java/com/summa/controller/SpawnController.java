package com.summa.controller;

import com.summa.service.SpawnService;
import com.summa.model.SpawnRequest;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/spawn")
public class SpawnController {
    private final SpawnService spawnService;
    private final AuditService auditService;

    public SpawnController(SpawnService spawnService, AuditService auditService) {
        this.spawnService = spawnService;
        this.auditService = auditService;
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
    public ResponseEntity<?> createRequest(@RequestBody Map<String, String> body,
                                            @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            SpawnRequest request = spawnService.create(
                body.get("requesterId"),
                body.get("templateId"),
                body.get("customRole"),
                body.get("class"),
                body.get("purpose"),
                body.get("workspaceBindings"),
                body.get("scopeCeiling"),
                body.containsKey("budgetCap") ? Double.parseDouble(body.get("budgetCap")) : null,
                body.containsKey("ttlHours") ? Integer.parseInt(body.get("ttlHours")) : null,
                body.get("requestedByHumanId"),
                actor
            );
            return ResponseEntity.ok(request);
        } catch (IllegalStateException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
        } catch (Exception e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.badRequest().body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable String id,
                                       @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
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
    public ResponseEntity<?> deny(@PathVariable String id,
                                    @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
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
}
