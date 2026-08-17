package com.summa.controller;

import com.summa.service.RunService;
import com.summa.model.Run;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/runs")
public class RunController {
    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startRun(@PathVariable String id) {
        try {
            Run run = runService.start(id);
            return ResponseEntity.ok(run);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
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
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<?> failRun(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            Run run = runService.fail(id, body.get("errorMessage"));
            return ResponseEntity.ok(run);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelRun(@PathVariable String id) {
        try {
            Run run = runService.cancel(id);
            return ResponseEntity.ok(run);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
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
