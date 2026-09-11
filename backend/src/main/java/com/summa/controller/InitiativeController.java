package com.summa.controller;

import com.summa.service.InitiativeService;
import com.summa.model.Initiative;
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
@RequestMapping("/initiatives")
public class InitiativeController {
    private final InitiativeService initiativeService;
    private final AuditService auditService;
    private final WriteGate writeGate;

    public InitiativeController(InitiativeService initiativeService, AuditService auditService, WriteGate writeGate) {
        this.initiativeService = initiativeService;
        this.auditService = auditService;
        this.writeGate = writeGate;
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
        Optional<Initiative> entOpt = initiativeService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Initiative not found: " + id);
    }

    @PostMapping
    public ResponseEntity<?> createInitiative(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            if (body.get("title") == null || body.get("title").isBlank()) {
                throw new IllegalArgumentException("title is required");
            }
            if (body.get("sponsor") == null || body.get("sponsor").isBlank()) {
                throw new IllegalArgumentException("sponsor is required");
            }
            if (body.get("lead") == null || body.get("lead").isBlank()) {
                throw new IllegalArgumentException("lead is required");
            }
            String generatedId = UUID.randomUUID().toString();
            Instant deadline = JsonHelpers.parseOptionalInstant(body.get("deadline"), "deadline");
            Initiative initiative = initiativeService.create(
                generatedId,
                body.get("title"),
                body.get("sponsor"),
                body.get("lead"),
                body.get("goalRef"),
                body.get("decisionRef"),
                deadline,
                body.get("dependsOn")
            );
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activate(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Initiative initiative = initiativeService.activate(id, actor);
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<?> pause(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Initiative initiative = initiativeService.pause(id, actor);
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resume(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Initiative initiative = initiativeService.resume(id, actor);
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<?> close(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Initiative initiative = initiativeService.close(id, actor);
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }
}
