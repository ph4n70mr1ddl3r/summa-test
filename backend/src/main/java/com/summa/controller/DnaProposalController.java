package com.summa.controller;

import com.summa.service.DnaProposalService;
import com.summa.model.DnaProposal;
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
@RequestMapping("/dna")
public class DnaProposalController {
    private final DnaProposalService proposalService;
    private final AuditService auditService;
    private final WriteGate writeGate;

    public DnaProposalController(DnaProposalService proposalService, AuditService auditService, WriteGate writeGate) {
        this.proposalService = proposalService;
        this.auditService = auditService;
        this.writeGate = writeGate;
    }

    @GetMapping("/proposals")
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

    @GetMapping("/proposals/{id}")
    public ResponseEntity<?> getProposal(@PathVariable String id) {
        Optional<DnaProposal> entOpt = proposalService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Proposal not found: " + id);
    }

    @PostMapping("/proposals")
    public ResponseEntity<?> createProposal(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            String generatedId = UUID.randomUUID().toString();
            DnaProposal proposal = proposalService.create(
                generatedId,
                body.get("kind"),
                body.get("payload"),
                actor,
                body.get("provenance"),
                body.get("domainId")
            );
            return ResponseEntity.ok(proposal);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/proposals/{id}/review")
    public ResponseEntity<?> reviewProposal(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        // API-022: single review endpoint with action in body
        String action = body.get("action");
        if ("publish".equals(action)) {
            try {
                DnaProposal proposal = proposalService.publish(id, actor, actor);
                return ResponseEntity.ok(proposal);
            } catch (IllegalArgumentException e) {
                return ControllerResponses.validation(auditService, e.getMessage());
            } catch (IllegalStateException e) {
                return ControllerResponses.gate(auditService, e.getMessage());
            }
        } else if ("reject".equals(action)) {
            try {
                DnaProposal proposal = proposalService.reject(id, actor, actor);
                return ResponseEntity.ok(proposal);
            } catch (IllegalArgumentException e) {
                return ControllerResponses.validation(auditService, e.getMessage());
            } catch (IllegalStateException e) {
                return ControllerResponses.gate(auditService, e.getMessage());
            }
        } else {
            return ControllerResponses.validation(auditService, "action must be 'publish' or 'reject'");
        }
    }

    @PostMapping("/proposals/{id}/withdraw")
    public ResponseEntity<?> withdrawProposal(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaProposal proposal = proposalService.withdraw(id, actor);
            return ResponseEntity.ok(proposal);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }

    @PostMapping("/proposals/{id}/amend")
    public ResponseEntity<?> amendProposal(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaProposal proposal = proposalService.amend(id, body.get("payload"), actor);
            return ResponseEntity.ok(proposal);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
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
