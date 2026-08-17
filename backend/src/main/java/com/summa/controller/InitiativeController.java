package com.summa.controller;

import com.summa.service.InitiativeService;
import com.summa.model.Initiative;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/initiatives")
public class InitiativeController {
    private final InitiativeService initiativeService;

    public InitiativeController(InitiativeService initiativeService) {
        this.initiativeService = initiativeService;
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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activate(@PathVariable String id,
                                       @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Initiative initiative = initiativeService.activate(id, actor);
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<?> pause(@PathVariable String id,
                                    @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Initiative initiative = initiativeService.pause(id, actor);
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resume(@PathVariable String id,
                                     @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Initiative initiative = initiativeService.resume(id, actor);
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<?> close(@PathVariable String id,
                                    @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Initiative initiative = initiativeService.close(id, actor);
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
