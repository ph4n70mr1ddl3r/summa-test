package com.summa.controller;

import com.summa.service.AgentService;
import com.summa.model.Agent;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/agents")
public class AgentController {
    private final AgentService agentService;
    private final AuditService auditService;

    public AgentController(AgentService agentService, AuditService auditService) {
        this.agentService = agentService;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<Agent>> listAgents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ownerId) {
        if (status != null) {
            return ResponseEntity.ok(agentService.findAll().stream()
                .filter(a -> status.equals(a.getStatus()))
                .toList());
        }
        if (ownerId != null) {
            return ResponseEntity.ok(agentService.findByOwner(ownerId));
        }
        return ResponseEntity.ok(agentService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAgent(@PathVariable String id) {
        return agentService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/lineage")
    public ResponseEntity<List<String>> getLineage(@PathVariable String id) {
        List<String> lineage = new java.util.ArrayList<>();
        String currentId = id;
        while (currentId != null && lineage.size() < 10) {
            lineage.add(currentId);
            Optional<Agent> agentOpt = agentService.findById(currentId);
            if (agentOpt.isPresent()) {
                currentId = agentOpt.get().getSpawnedBy();
            } else {
                currentId = null;
            }
        }
        return ResponseEntity.ok(lineage);
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<?> suspend(@PathVariable String id,
                                       @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Agent agent = agentService.suspend(id, actor);
            return ResponseEntity.ok(agent);
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

    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resume(@PathVariable String id,
                                      @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Agent agent = agentService.resume(id, actor);
            return ResponseEntity.ok(agent);
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

    @PostMapping("/{id}/retire")
    public ResponseEntity<?> retire(@PathVariable String id,
                                      @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Agent agent = agentService.retire(id, actor);
            return ResponseEntity.ok(agent);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archive(@PathVariable String id,
                                       @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Agent agent = agentService.archive(id, actor);
            return ResponseEntity.ok(agent);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/promote")
    public ResponseEntity<?> promote(@PathVariable String id, @RequestBody Map<String, String> body,
                                      @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        // API-033: files promotion ask for customRole hire
        // Stub: in production this creates a proposal-shaped ask per TPL-040..046
        return ResponseEntity.ok(Map.of("status", "promotion_ask_filed", "agentId", id));
    }
}
