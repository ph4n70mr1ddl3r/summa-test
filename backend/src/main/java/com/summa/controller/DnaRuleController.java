package com.summa.controller;

import com.summa.service.DnaRuleService;
import com.summa.model.DnaRule;
import com.summa.service.AuditService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.util.JsonHelpers;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/dna/rules")
public class DnaRuleController {
    private final DnaRuleService ruleService;
    private final AuditService auditService;
    private final WriteGate writeGate;

    public DnaRuleController(DnaRuleService ruleService, AuditService auditService, WriteGate writeGate) {
        this.ruleService = ruleService;
        this.auditService = auditService;
        this.writeGate = writeGate;
    }

    @GetMapping
    public ResponseEntity<List<DnaRule>> listRules(
            @RequestParam(required = false) String domainId) {
        if (domainId != null) {
            return ResponseEntity.ok(ruleService.findByDomain(domainId));
        }
        return ResponseEntity.ok(ruleService.findAllActiveWindowed(Instant.now()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRule(@PathVariable String id) {
        return ruleService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createRule(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Instant effectiveFrom = body.containsKey("effectiveFrom") && body.get("effectiveFrom") != null && !body.get("effectiveFrom").isBlank() ?
                JsonHelpers.parseOptionalInstant(body.get("effectiveFrom"), "effectiveFrom") : Instant.now();
            Instant effectiveTo = body.containsKey("effectiveTo") && body.get("effectiveTo") != null && !body.get("effectiveTo").isBlank() ?
                JsonHelpers.parseOptionalInstant(body.get("effectiveTo"), "effectiveTo") : null;

            // Security: reject client-supplied IDs — always generate server-side
            String generatedId = UUID.randomUUID().toString();
            DnaRule rule = ruleService.create(
                generatedId,
                body.get("domainId"),
                body.get("statementMd"),
                body.get("machineHint"),
                effectiveFrom,
                effectiveTo,
                body.get("supersedesId"),
                actor
            );
            return ResponseEntity.ok(rule);
        } catch (IllegalArgumentException | DateTimeException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateRule(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Instant effectiveTo = body.containsKey("effectiveTo") && body.get("effectiveTo") != null && !body.get("effectiveTo").isBlank() ?
                JsonHelpers.parseOptionalInstant(body.get("effectiveTo"), "effectiveTo") : null;

            DnaRule rule = ruleService.update(
                id,
                body.get("statementMd"),
                body.get("machineHint"),
                effectiveTo,
                actor
            );
            return ResponseEntity.ok(rule);
        } catch (IllegalArgumentException | DateTimeException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/supersede/{supersedesId}")
    public ResponseEntity<?> supersede(@PathVariable String id, @PathVariable String supersedesId) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaRule rule = ruleService.supersede(id, supersedesId, actor);
            return ResponseEntity.ok(rule);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        }
    }
}
