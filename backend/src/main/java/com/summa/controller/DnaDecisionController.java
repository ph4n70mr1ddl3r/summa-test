package com.summa.controller;

import com.summa.service.DnaDecisionService;
import com.summa.model.DnaDecision;
import com.summa.service.AuditService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/dna/decisions")
public class DnaDecisionController {
    private final DnaDecisionService decisionService;
    private final AuditService auditService;
    private final WriteGate writeGate;

    public DnaDecisionController(DnaDecisionService decisionService, AuditService auditService, WriteGate writeGate) {
        this.decisionService = decisionService;
        this.auditService = auditService;
        this.writeGate = writeGate;
    }

    @GetMapping
    public ResponseEntity<List<DnaDecision>> listDecisions(
            @RequestParam(required = false) String domainId) {
        if (domainId != null) {
            return ResponseEntity.ok(decisionService.findByDomain(domainId));
        }
        return ResponseEntity.ok(decisionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDecision(@PathVariable String id) {
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
            if (body.get("contextMd") == null || body.get("contextMd").isBlank()) {
                throw new IllegalArgumentException("contextMd is required");
            }
            if (body.get("outcomeMd") == null || body.get("outcomeMd").isBlank()) {
                throw new IllegalArgumentException("outcomeMd is required");
            }
            if (body.get("decidedBy") == null || body.get("decidedBy").isBlank()) {
                throw new IllegalArgumentException("decidedBy is required");
            }
            String generatedId = UUID.randomUUID().toString();
            DnaDecision decision = decisionService.create(
                generatedId,
                body.get("domainId"),
                body.get("contextMd"),
                body.get("outcomeMd"),
                body.get("decidedBy"),
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
