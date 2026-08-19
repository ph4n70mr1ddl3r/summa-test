package com.summa.controller;

import com.summa.service.DnaRuleService;
import com.summa.model.DnaRule;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Instant effectiveFrom = body.containsKey("effectiveFrom") ? 
                Instant.parse(body.get("effectiveFrom")) : Instant.now();
            Instant effectiveTo = body.containsKey("effectiveTo") ? 
                Instant.parse(body.get("effectiveTo")) : null;
            
            DnaRule rule = ruleService.create(
                body.get("id"),
                body.get("domainId"),
                body.get("statementMd"),
                body.get("machineHint"),
                effectiveFrom,
                effectiveTo,
                body.get("supersedesId"),
                actor
            );
            return ResponseEntity.ok(rule);
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

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateRule(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Instant effectiveTo = body.containsKey("effectiveTo") ? 
                Instant.parse(body.get("effectiveTo")) : null;
            
            DnaRule rule = ruleService.update(
                id,
                body.get("statementMd"),
                body.get("machineHint"),
                effectiveTo,
                actor
            );
            return ResponseEntity.ok(rule);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        } catch (IllegalStateException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/supersede/{supersedesId}")
    public ResponseEntity<?> supersede(@PathVariable String id, @PathVariable String supersedesId) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaRule rule = ruleService.supersede(id, supersedesId, actor);
            return ResponseEntity.ok(rule);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }
}
