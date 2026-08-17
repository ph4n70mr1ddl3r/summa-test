package com.summa.controller;

import com.summa.service.DnaGoalService;
import com.summa.model.DnaGoal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dna/goals")
public class DnaGoalController {
    private final DnaGoalService goalService;

    public DnaGoalController(DnaGoalService goalService) {
        this.goalService = goalService;
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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body,
                                           @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaGoal goal = goalService.updateStatus(id, body.get("status"), actor);
            return ResponseEntity.ok(goal);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
            return ResponseEntity.notFound().build();
        }
    }
}
