package com.summa.controller;

import com.summa.service.DnaGoalService;
import com.summa.model.DnaGoal;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dna/goals")
public class DnaGoalController {
    private final DnaGoalService goalService;
    private final AuditService auditService;

    public DnaGoalController(DnaGoalService goalService, AuditService auditService) {
        this.goalService = goalService;
        this.auditService = auditService;
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
    public ResponseEntity<?> createGoal(@RequestBody Map<String, String> body,
                                         @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Instant effectiveFrom = body.containsKey("effectiveFrom") ?
                Instant.parse(body.get("effectiveFrom")) : Instant.now();
            Instant effectiveTo = body.containsKey("effectiveTo") ?
                Instant.parse(body.get("effectiveTo")) : null;

            DnaGoal goal = goalService.create(
                body.get("id"),
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
        } catch (Exception e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.badRequest().body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body,
                                           @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
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
    public ResponseEntity<?> updateWindow(@PathVariable String id, @RequestBody Map<String, String> body,
                                           @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Instant effectiveFrom = body.containsKey("effectiveFrom") ?
                Instant.parse(body.get("effectiveFrom")) : null;
            Instant effectiveTo = body.containsKey("effectiveTo") ?
                Instant.parse(body.get("effectiveTo")) : null;

            DnaGoal goal = goalService.updateWindow(id, effectiveFrom, effectiveTo, actor);
            return ResponseEntity.ok(goal);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }
}
