package com.summa.controller;

import com.summa.service.DnaProposalService;
import com.summa.model.DnaProposal;
import com.summa.service.AuditService;
import com.summa.service.MemberService;
import com.summa.security.WriteGate;
import com.summa.constants.Defaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.enums.RbacRole;
import com.summa.security.RbacAuthorizationFilter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/dna/proposals")
public class DnaProposalController {
    private static final Set<String> VALID_PROPOSAL_KINDS = Set.of("card", "rule", "decision", "goal", "glossary", "edit");
    private final DnaProposalService proposalService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final MemberService memberService;

    public DnaProposalController(DnaProposalService proposalService, AuditService auditService, WriteGate writeGate,
                                 MemberService memberService) {
        this.proposalService = proposalService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<List<DnaProposal>> listProposals(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String domainId) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        if (!"system".equals(actor)) {
            Optional<com.summa.model.Human> humanOpt = memberService.findHuman(actor);
            Optional<com.summa.model.Agent> agentOpt = memberService.findAgent(actor);
            boolean hasAccess = humanOpt.isPresent() || agentOpt.isPresent();
            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
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
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        if (!"system".equals(actor)) {
            Optional<com.summa.model.Human> humanOpt = memberService.findHuman(actor);
            Optional<com.summa.model.Agent> agentOpt = memberService.findAgent(actor);
            boolean hasAccess = humanOpt.isPresent() || agentOpt.isPresent();
            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        Optional<DnaProposal> entOpt = proposalService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Proposal not found: " + id);
    }

    @PostMapping
    public ResponseEntity<?> createProposal(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            String generatedId = UUID.randomUUID().toString();
            String kind = body.get("kind");
            if (kind == null || kind.isBlank()) {
                throw new IllegalArgumentException("kind is required");
            }
            if (!VALID_PROPOSAL_KINDS.contains(kind)) {
                throw new IllegalArgumentException("Invalid kind: " + kind + ". Must be one of: " + VALID_PROPOSAL_KINDS);
            }
            String payload = body.get("payload");
            if (payload == null || payload.isBlank()) {
                throw new IllegalArgumentException("payload is required");
            }
            DnaProposal proposal = proposalService.create(
                generatedId,
                kind,
                payload,
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

    @PostMapping("/{id}/review")
    public ResponseEntity<?> reviewProposal(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        // API-022: single review endpoint with action in body
        String action = body.get("action");
        // Validate reviewedBy — only the actor or an admin may set it; otherwise defaults to actor
        String reviewedBy = body.get("reviewedBy");
        String effectiveReviewer = actor;
        if (reviewedBy != null && !reviewedBy.isBlank()) {
            if (!reviewedBy.equals(actor)) {
                Optional<com.summa.model.Human> reviewerOpt = memberService.findHuman(reviewedBy.replaceFirst("^[ha]?:", ""));
                boolean isReviewerAdmin = reviewerOpt.isPresent() && RbacRole.ADMIN.getValue().equals(reviewerOpt.get().getRbac());
                if (!isReviewerAdmin) {
                    return ControllerResponses.validation(auditService, "reviewedBy must be the current actor or an admin");
                }
                effectiveReviewer = reviewedBy.replaceFirst("^[ha]?:", "");
            }
        }
        if ("publish".equals(action)) {
            try {
                DnaProposal proposal = proposalService.publish(id, effectiveReviewer, actor);
                return ResponseEntity.ok(proposal);
            } catch (IllegalArgumentException e) {
                return ControllerResponses.validation(auditService, e.getMessage());
            } catch (IllegalStateException e) {
                return ControllerResponses.gate(auditService, e.getMessage());
            }
        } else if ("reject".equals(action)) {
            try {
                DnaProposal proposal = proposalService.reject(id, effectiveReviewer, actor);
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

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdrawProposal(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaProposal proposal = proposalService.withdraw(id, actor);
            return ResponseEntity.ok(proposal);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/amend")
    public ResponseEntity<?> amendProposal(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaProposal proposal = proposalService.amend(id, body.get("payload"), actor);
            return ResponseEntity.ok(proposal);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @GetMapping("/review-queue")
    public ResponseEntity<?> reviewQueue(@RequestParam(required = false) String domainId) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        if (actor == null || Defaults.SYSTEM_ACTOR.equals(actor)) {
            return ControllerResponses.gate(auditService, "Authentication required");
        }
        // API-022: GET /dna/proposals/review-queue
        if (domainId != null) {
            return ResponseEntity.ok(proposalService.findOpenByDomain(domainId));
        }
        return ResponseEntity.ok(proposalService.findAllOpen());
    }
}
