package com.summa.controller;

import com.summa.service.DnaRuleService;
import com.summa.model.DnaRule;
import com.summa.service.AuditService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.constants.Defaults;
import com.summa.util.JsonHelpers;
import com.summa.service.DnaDomainService;
import com.summa.service.MemberService;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/dna/rules")
public class DnaRuleController {
    private final DnaRuleService ruleService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final DnaDomainService domainService;
    private final MemberService memberService;

    public DnaRuleController(DnaRuleService ruleService, AuditService auditService, WriteGate writeGate,
                              DnaDomainService domainService, MemberService memberService) {
        this.ruleService = ruleService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.domainService = domainService;
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<?> listRules(
            @RequestParam(required = false) String domainId) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        if (domainId != null) {
            return ResponseEntity.ok(ruleService.findByDomain(domainId));
        }
        return ResponseEntity.ok(ruleService.findAllActiveWindowed(Instant.now()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRule(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        Optional<DnaRule> entOpt = ruleService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Rule not found: " + id);
    }

    @PostMapping
    public ResponseEntity<?> createRule(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            String domainId = body.get("domainId");
            if (domainId == null || domainId.isBlank()) {
                throw new IllegalArgumentException("domainId is required");
            }
            if (domainService.findById(domainId).isEmpty()) {
                throw new IllegalArgumentException("Domain not found: " + domainId);
            }
            String statementMd = body.get("statementMd");
            if (statementMd == null || statementMd.isBlank()) {
                throw new IllegalArgumentException("statementMd is required");
            }
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
        } catch (IllegalArgumentException e) {
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
        } catch (IllegalArgumentException e) {
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
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }
}
