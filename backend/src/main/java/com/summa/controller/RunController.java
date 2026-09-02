package com.summa.controller;

import com.summa.service.RunService;
import com.summa.model.Run;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import com.summa.security.WriteGate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/runs")
public class RunController {
    private final RunService runService;
    private final AuditService auditService;
    private final WriteGate writeGate;

    public RunController(RunService runService, AuditService auditService, WriteGate writeGate) {
        this.runService = runService;
        this.auditService = auditService;
        this.writeGate = writeGate;
    }

    @GetMapping
    public ResponseEntity<List<Run>> listRuns(
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
        List<Run> all;
        if (agentId != null) {
            all = runService.findByAgent(agentId);
        } else if (workspaceId != null) {
            all = runService.findByWorkspace(workspaceId);
        } else if (status != null) {
            all = runService.findByStatus(status);
        } else {
            all = runService.findRecent(limit);
        }
        return ResponseEntity.ok(all.stream().limit(limit).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRun(@PathVariable String id) {
        return runService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createRun(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Run run = runService.create(
                body.get("agentId"),
                body.get("workspaceId"),
                body.get("initiativeId"),
                body.get("triggerId"),
                body.get("prompt"),
                actor
            );
            return ResponseEntity.ok(run);
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

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startRun(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Run run = runService.start(id);
            return ResponseEntity.ok(run);
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

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeRun(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            String result = body.get("result");
            Long costTokens = body.containsKey("costTokens") && body.get("costTokens") != null ?
                Long.parseLong(body.get("costTokens")) : null;
            Double costUsd = body.containsKey("costUsd") && body.get("costUsd") != null ?
                Double.parseDouble(body.get("costUsd")) : null;

            Run run = runService.complete(id, result, costTokens, costUsd);
            return ResponseEntity.ok(run);
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

    @PostMapping("/{id}/fail")
    public ResponseEntity<?> failRun(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Run run = runService.fail(id, body.get("errorMessage"));
            return ResponseEntity.ok(run);
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

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelRun(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Run run = runService.cancel(id);
            return ResponseEntity.ok(run);
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

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of(
            "queued", runService.countByStatus("queued"),
            "running", runService.countByStatus("running"),
            "completed", runService.countByStatus("completed"),
            "failed", runService.countByStatus("failed")
        ));
    }
}
