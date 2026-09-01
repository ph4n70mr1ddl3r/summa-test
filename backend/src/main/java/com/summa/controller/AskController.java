package com.summa.controller;

import com.summa.service.AskService;
import com.summa.model.Ask;
import com.summa.service.AuditService;
import com.summa.security.WriteGate;
import com.summa.security.RbacAuthorizationFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/asks")
public class AskController {
    private final AskService askService;
    private final AuditService auditService;
    private final WriteGate writeGate;

    public AskController(AskService askService, AuditService auditService, WriteGate writeGate) {
        this.askService = askService;
        this.auditService = auditService;
        this.writeGate = writeGate;
    }

    @GetMapping
    public ResponseEntity<List<Ask>> listAsks(
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String status) {
        if (to != null) {
            return ResponseEntity.ok(askService.findByTo(to));
        }
        if (status != null) {
            return ResponseEntity.ok(askService.findByStatus(status));
        }
        return ResponseEntity.ok(askService.findAllPending());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAsk(@PathVariable String id) {
        return askService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createAsk(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            String deadlineStr = body.get("deadlineSeconds");
            long deadlineSeconds = 86400L;
            if (deadlineStr != null && !deadlineStr.isBlank()) {
                try {
                    deadlineSeconds = Long.parseLong(deadlineStr);
                } catch (NumberFormatException e) {
                    return ControllerResponses.validation(auditService, "Invalid deadlineSeconds value");
                }
            }
            Instant deadline;
            try {
                deadline = Instant.now().plusSeconds(deadlineSeconds);
            } catch (Exception e) {
                return ControllerResponses.validation(auditService, "Invalid deadlineSeconds value");
            }
            Ask ask = askService.create(
                body.get("kind"),
                actor,
                body.get("to"),
                body.get("payload"),
                body.get("slaTier"),
                body.get("expiryBehavior"),
                body.containsKey("quorumRequired") && body.get("quorumRequired") != null ? parseIntSafe(body.get("quorumRequired")) : null,
                deadline,
                body.get("initiativeId"),
                body.get("workspaceId")
            );
            return ResponseEntity.ok(ask);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<?> respond(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Ask ask = askService.respond(id, actor, body.get("response"));
            return ResponseEntity.ok(ask);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Ask ask = askService.withdraw(id, actor);
            return ResponseEntity.ok(ask);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/expire")
    public ResponseEntity<?> expire(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Ask ask = askService.expire(id);
            return ResponseEntity.ok(ask);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        }
    }

    private Integer parseIntSafe(String s) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid quorumRequired: " + s); }
    }
}
