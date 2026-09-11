package com.summa.controller;

import com.summa.service.DnaGoalService;
import com.summa.model.DnaGoal;
import com.summa.service.AuditService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.util.JsonHelpers;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
        Optional<DnaGoal> entOpt = goalService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Goal not found: " + id);
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
            String generatedId = UUID.randomUUID().toString();
            Instant effectiveFrom;
            try {
                effectiveFrom = JsonHelpers.parseOptionalInstant(body.get("effectiveFrom"), "effectiveFrom");
                if (effectiveFrom == null) effectiveFrom = Instant.now();
            } catch (IllegalArgumentException e) {
                throw e;
            }
            Instant effectiveTo = JsonHelpers.parseOptionalInstant(body.get("effectiveTo"), "effectiveTo");

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
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
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
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
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
            effectiveFrom = JsonHelpers.parseOptionalInstant(body.get("effectiveFrom"), "effectiveFrom");
            effectiveTo = JsonHelpers.parseOptionalInstant(body.get("effectiveTo"), "effectiveTo");
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, "Invalid date format: " + e.getMessage());
        }
        try {
            DnaGoal goal = goalService.updateWindow(id, effectiveFrom, effectiveTo, actor);
            return ResponseEntity.ok(goal);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        }
    }
}
