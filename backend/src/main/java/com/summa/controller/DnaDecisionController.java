package com.summa.controller;

import com.summa.service.DnaDecisionService;
import com.summa.model.DnaDecision;
import com.summa.model.Human;
import com.summa.model.Agent;
import com.summa.service.AuditService;
import com.summa.service.MemberService;
import com.summa.service.DnaDomainService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.exception.EntityNotFoundException;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.constants.Defaults;
import com.summa.util.JsonHelpers;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/dna/decisions")
public class DnaDecisionController {
    private final DnaDecisionService decisionService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final MemberService memberService;
    private final DnaDomainService domainService;

    public DnaDecisionController(DnaDecisionService decisionService, AuditService auditService, WriteGate writeGate,
                                   MemberService memberService, DnaDomainService domainService) {
        this.decisionService = decisionService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.memberService = memberService;
        this.domainService = domainService;
    }

    @GetMapping
    public ResponseEntity<?> listDecisions(
            @RequestParam(required = false) String domainId) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        if (domainId != null) {
            return ResponseEntity.ok(decisionService.findByDomain(domainId));
        }
        return ResponseEntity.ok(decisionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDecision(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        Optional<DnaDecision> entOpt = decisionService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Decision not found: " + id);
    }

    @PostMapping
    public ResponseEntity<?> createDecision(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            if (body.get("domainId") == null || body.get("domainId").isBlank()) {
                throw new IllegalArgumentException("domainId is required");
            }
            if (domainService.findById(body.get("domainId")).isEmpty()) {
                throw new EntityNotFoundException("Domain not found: " + body.get("domainId"));
            }
            if (body.get("contextMd") == null || body.get("contextMd").isBlank()) {
                throw new IllegalArgumentException("contextMd is required");
            }
            if (body.get("outcomeMd") == null || body.get("outcomeMd").isBlank()) {
                throw new IllegalArgumentException("outcomeMd is required");
            }
            String generatedId = UUID.randomUUID().toString();
            String actorClean = JsonHelpers.stripIdPrefix(actor);
            // Validate that the authenticated actor references an existing human or agent
            Optional<Human> deciderHuman = memberService.findHuman(actorClean);
            Optional<Agent> deciderAgent = memberService.findAgent(actorClean);
            if (deciderHuman.isEmpty() && deciderAgent.isEmpty()) {
                throw new IllegalArgumentException("Authenticated actor is not a valid human or agent: " + actorClean);
            }
            DnaDecision decision = decisionService.create(
                generatedId,
                body.get("domainId"),
                body.get("contextMd"),
                body.get("outcomeMd"),
                actorClean,
                body.get("provenance"),
                actor
            );
            return ResponseEntity.ok(decision);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }
}
