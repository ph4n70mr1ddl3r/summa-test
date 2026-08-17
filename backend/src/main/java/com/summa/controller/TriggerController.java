package com.summa.controller;

import com.summa.service.TriggerService;
import com.summa.model.Trigger;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/triggers")
public class TriggerController {
    private final TriggerService triggerService;
    private final AuditService auditService;

    public TriggerController(TriggerService triggerService, AuditService auditService) {
        this.triggerService = triggerService;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<Trigger>> listTriggers(
            @RequestParam(required = false) String agentId) {
        if (agentId != null) {
            return ResponseEntity.ok(triggerService.findByAgent(agentId));
        }
        return ResponseEntity.ok(triggerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTrigger(@PathVariable String id) {
        return triggerService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createTrigger(@RequestBody Map<String, String> body,
                                            @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Trigger trigger = triggerService.create(
                body.get("name"),
                body.get("kind"),
                body.get("expression"),
                body.get("agentId"),
                body.get("workspaceId"),
                body.get("criticality"),
                body.get("config"),
                actor
            );
            return ResponseEntity.ok(trigger);
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

    @PostMapping("/{id}/pause")
    public ResponseEntity<?> pause(@PathVariable String id,
                                    @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Trigger trigger = triggerService.pause(id, actor);
            return ResponseEntity.ok(trigger);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resume(@PathVariable String id,
                                     @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Trigger trigger = triggerService.resume(id, actor);
            return ResponseEntity.ok(trigger);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archive(@PathVariable String id,
                                      @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Trigger trigger = triggerService.archive(id, actor);
            return ResponseEntity.ok(trigger);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(triggerService.getStats());
    }
}
