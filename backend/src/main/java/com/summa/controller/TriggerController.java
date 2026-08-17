package com.summa.controller;

import com.summa.service.TriggerService;
import com.summa.model.Trigger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/triggers")
public class TriggerController {
    private final TriggerService triggerService;

    public TriggerController(TriggerService triggerService) {
        this.triggerService = triggerService;
    }

    @GetMapping
    public ResponseEntity<List<Trigger>> listTriggers(
            @RequestParam(required = false) String agentId) {
        if (agentId != null) {
            return ResponseEntity.ok(triggerService.findByAgent(agentId));
        }
        return ResponseEntity.ok(triggerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTrigger(@PathVariable String id) {
        return triggerService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createTrigger(@RequestBody Map<String, String> body,
                                            @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Trigger trigger = triggerService.create(
                body.get("name"),
                body.get("kind"),
                body.get("expression"),
                body.get("agentId"),
                body.get("workspaceId"),
                body.get("criticality"),
                body.get("config"),
                actor
            );
            return ResponseEntity.ok(trigger);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<?> pause(@PathVariable String id,
                                    @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Trigger trigger = triggerService.pause(id, actor);
            return ResponseEntity.ok(trigger);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resume(@PathVariable String id,
                                     @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Trigger trigger = triggerService.resume(id, actor);
            return ResponseEntity.ok(trigger);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archive(@PathVariable String id,
                                      @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Trigger trigger = triggerService.archive(id, actor);
            return ResponseEntity.ok(trigger);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(triggerService.getStats());
    }
}
