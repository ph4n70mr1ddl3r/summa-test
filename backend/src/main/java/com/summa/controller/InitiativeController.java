package com.summa.controller;

import com.summa.service.InitiativeService;
import com.summa.model.Initiative;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/initiatives")
public class InitiativeController {
    private final InitiativeService initiativeService;
    private final AuditService auditService;

    public InitiativeController(InitiativeService initiativeService, AuditService auditService) {
        this.initiativeService = initiativeService;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<Initiative>> listInitiatives(
            @RequestParam(required = false) String status) {
        if (status != null) {
            return ResponseEntity.ok(initiativeService.findByStatus(status));
        }
        return ResponseEntity.ok(initiativeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getInitiative(@PathVariable String id) {
        return initiativeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createInitiative(@RequestBody Map<String, String> body,
                                               @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Instant deadline = body.containsKey("deadline") ? 
                Instant.parse(body.get("deadline")) : null;
            Initiative initiative = initiativeService.create(
                body.get("id"),
                body.get("title"),
                body.get("sponsor"),
                body.get("lead"),
                body.get("goalRef"),
                body.get("decisionRef"),
                deadline,
                body.get("dependsOn")
            );
            return ResponseEntity.ok(initiative);
        } catch (Exception e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.badRequest().body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activate(@PathVariable String id,
                                       @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Initiative initiative = initiativeService.activate(id, actor);
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
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
            Initiative initiative = initiativeService.pause(id, actor);
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        } catch (IllegalStateException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resume(@PathVariable String id,
                                     @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Initiative initiative = initiativeService.resume(id, actor);
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        } catch (IllegalStateException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<?> close(@PathVariable String id,
                                    @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Initiative initiative = initiativeService.close(id, actor);
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        } catch (IllegalStateException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }
}
