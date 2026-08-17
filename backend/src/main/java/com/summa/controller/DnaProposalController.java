package com.summa.controller;

import com.summa.service.DnaProposalService;
import com.summa.model.DnaProposal;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dna/proposals")
public class DnaProposalController {
    private final DnaProposalService proposalService;
    private final AuditService auditService;

    public DnaProposalController(DnaProposalService proposalService, AuditService auditService) {
        this.proposalService = proposalService;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<DnaProposal>> listProposals(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String domainId) {
        if (status != null) {
            return ResponseEntity.ok(proposalService.findByStatus(status));
        }
        if (domainId != null) {
            return ResponseEntity.ok(proposalService.findOpenByDomain(domainId));
        }
        return ResponseEntity.ok(proposalService.findAllOpen());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProposal(@PathVariable String id) {
        return proposalService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createProposal(@RequestBody Map<String, String> body,
                                             @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaProposal proposal = proposalService.create(
                body.get("id"),
                body.get("kind"),
                body.get("payload"),
                actor,
                body.get("provenance"),
                body.get("domainId")
            );
            return ResponseEntity.ok(proposal);
        } catch (Exception e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.badRequest().body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<?> reviewProposal(@PathVariable String id, @RequestBody Map<String, String> body,
                                             @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        // API-022: single review endpoint with action in body
        String action = body.get("action");
        if ("publish".equals(action)) {
            try {
                DnaProposal proposal = proposalService.publish(id, actor, actor);
                return ResponseEntity.ok(proposal);
            } catch (IllegalArgumentException e) {
                AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
            } catch (IllegalStateException e) {
                AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
            }
        } else if ("reject".equals(action)) {
            try {
                DnaProposal proposal = proposalService.reject(id, actor, actor);
                return ResponseEntity.ok(proposal);
            } catch (IllegalArgumentException e) {
                AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
            }
        } else {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", "action must be 'publish' or 'reject'", null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("code", "validation", "message", "action must be 'publish' or 'reject'", "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdrawProposal(@PathVariable String id,
                                               @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaProposal proposal = proposalService.withdraw(id, actor);
            return ResponseEntity.ok(proposal);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/amend")
    public ResponseEntity<?> amendProposal(@PathVariable String id, @RequestBody Map<String, String> body,
                                            @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaProposal proposal = proposalService.amend(id, body.get("payload"), actor);
            return ResponseEntity.ok(proposal);
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

    @GetMapping("/review-queue")
    public ResponseEntity<List<DnaProposal>> reviewQueue(@RequestParam(required = false) String domainId) {
        // API-022: GET /dna/review-queue
        if (domainId != null) {
            return ResponseEntity.ok(proposalService.findOpenByDomain(domainId));
        }
        return ResponseEntity.ok(proposalService.findAllOpen());
    }
}
