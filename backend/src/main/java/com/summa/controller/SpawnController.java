package com.summa.controller;

import com.summa.service.SpawnService;
import com.summa.model.SpawnRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/spawn")
public class SpawnController {
    private final SpawnService spawnService;

    public SpawnController(SpawnService spawnService) {
        this.spawnService = spawnService;
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
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable String id,
                                      @RequestHeader(value = "X-Actor") String actor) {
        try {
            SpawnRequest request = spawnService.approve(id, actor, actor);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/deny")
    public ResponseEntity<?> deny(@PathVariable String id,
                                   @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            SpawnRequest request = spawnService.deny(id, actor);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(spawnService.getStats());
    }
}
