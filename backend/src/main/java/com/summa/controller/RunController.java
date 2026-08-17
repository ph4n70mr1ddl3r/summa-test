package com.summa.controller;

import com.summa.service.RunService;
import com.summa.model.Run;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/runs")
public class RunController {
    private final RunService runService;
    private final AuditService auditService;

    public RunController(RunService runService, AuditService auditService) {
        this.runService = runService;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<Run>> listRuns(
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        if (agentId != null) {
            return ResponseEntity.ok(runService.findByAgent(agentId).stream().limit(limit).toList());
        }
        if (workspaceId != null) {
            return ResponseEntity.ok(runService.findByWorkspace(workspaceId).stream().limit(limit).toList());
        }
        if (status != null) {
            return ResponseEntity.ok(runService.findRecent(limit).stream()
                .filter(r -> status.equals(r.getStatus()))
                .toList());
        }
        return ResponseEntity.ok(runService.findRecent(limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRun(@PathVariable String id) {
        return runService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createRun(@RequestBody Map<String, String> body,
                                        @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
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
        } catch (Exception e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.badRequest().body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startRun(@PathVariable String id) {
        try {
            Run run = runService.start(id);
            return ResponseEntity.ok(run);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeRun(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            String result = (String) body.get("result");
            Long costTokens = body.get("costTokens") != null ? 
                ((Number) body.get("costTokens")).longValue() : null;
            Double costUsd = body.get("costUsd") != null ? 
                ((Number) body.get("costUsd")).doubleValue() : null;
            
            Run run = runService.complete(id, result, costTokens, costUsd);
            return ResponseEntity.ok(run);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<?> failRun(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            Run run = runService.fail(id, body.get("errorMessage"));
            return ResponseEntity.ok(run);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelRun(@PathVariable String id) {
        try {
            Run run = runService.cancel(id);
            return ResponseEntity.ok(run);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
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
