package com.summa.controller;

import com.summa.service.DnaGoalService;
import com.summa.model.DnaGoal;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dna/goals")
public class DnaGoalController {
    private final DnaGoalService goalService;
    private final AuditService auditService;
    private final WriteGate writeGate;

    public DnaGoalController(DnaGoalService goalService, AuditService auditService, WriteGate writeGate) {
        this.goalService = goalService;
        this.auditService = auditService;
        this.writeGate = writeGate;
    }

    @GetMapping
    public ResponseEntity<List<DnaGoal>> listGoals(
            @RequestParam(required = false) String domainId,
            @RequestParam(required = false) String inject) {
        if (inject != null) {
            return ResponseEntity.ok(goalService.findActiveInject(inject, Instant.now()));
        }
        if (domainId != null) {
            return ResponseEntity.ok(goalService.findByDomain(domainId));
        }
        return ResponseEntity.ok(goalService.findAllActiveWindowed(Instant.now()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGoal(@PathVariable String id) {
        return goalService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createGoal(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            if (body.get("statementMd") == null || body.get("statementMd").isBlank()) {
                throw new IllegalArgumentException("statementMd is required");
            }
            if (body.get("owner") == null || body.get("owner").isBlank()) {
                throw new IllegalArgumentException("owner is required");
            }
            // Security: reject client-supplied IDs — always generate server-side
            String generatedId = java.util.UUID.randomUUID().toString();
            Instant effectiveFrom = null;
            if (body.containsKey("effectiveFrom") && body.get("effectiveFrom") != null && !body.get("effectiveFrom").isBlank()) {
                try {
                    effectiveFrom = Instant.parse(body.get("effectiveFrom"));
                } catch (DateTimeException e) {
                    throw new IllegalArgumentException("Invalid effectiveFrom format: " + body.get("effectiveFrom"));
                }
            } else {
                effectiveFrom = Instant.now();
            }
            Instant effectiveTo = null;
            if (body.containsKey("effectiveTo") && body.get("effectiveTo") != null && !body.get("effectiveTo").isBlank()) {
                try {
                    effectiveTo = Instant.parse(body.get("effectiveTo"));
                } catch (DateTimeException e) {
                    throw new IllegalArgumentException("Invalid effectiveTo format: " + body.get("effectiveTo"));
                }
            }

            DnaGoal goal = goalService.create(
                generatedId,
                body.get("domainId"),
                body.get("quarter"),
                body.get("statementMd"),
                body.get("owner"),
                body.get("inject"),
                effectiveFrom,
                effectiveTo,
                actor
            );
            return ResponseEntity.ok(goal);
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

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaGoal goal = goalService.updateStatus(id, body.get("status"), actor);
            return ResponseEntity.ok(goal);
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

    @PatchMapping("/{id}/window")
    public ResponseEntity<?> updateWindow(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        final Instant effectiveFrom;
        final Instant effectiveTo;
        try {
            effectiveFrom = parseOptionalInstant(body.get("effectiveFrom"), "effectiveFrom");
            effectiveTo = parseOptionalInstant(body.get("effectiveTo"), "effectiveTo");
        } catch (DateTimeException | IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "validation", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("code", "validation", "message", "Invalid date format: " + e.getMessage(), "audit_event_id", audit.getId()));
        }
        try {
            DnaGoal goal = goalService.updateWindow(id, effectiveFrom, effectiveTo, actor);
            return ResponseEntity.ok(goal);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    /**
     * Parse an optional ISO-8601 instant. Blank/missing values clear the field
     * (null) instead of throwing — {@code Instant.parse(null)} would NPE into a 500.
     */
    private static Instant parseOptionalInstant(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid " + field + " format: " + value);
        }
    }
}
