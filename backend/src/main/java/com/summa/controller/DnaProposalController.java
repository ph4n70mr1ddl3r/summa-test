package com.summa.controller;

import com.summa.service.DnaProposalService;
import com.summa.model.DnaProposal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dna/proposals")
public class DnaProposalController {
    private final DnaProposalService proposalService;

    public DnaProposalController(DnaProposalService proposalService) {
        this.proposalService = proposalService;
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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/review/publish")
    public ResponseEntity<?> publishProposal(@PathVariable String id,
                                              @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaProposal proposal = proposalService.publish(id, actor, actor);
            return ResponseEntity.ok(proposal);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/review/reject")
    public ResponseEntity<?> rejectProposal(@PathVariable String id,
                                             @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaProposal proposal = proposalService.reject(id, actor, actor);
            return ResponseEntity.ok(proposal);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdrawProposal(@PathVariable String id,
                                               @RequestHeader(value = "X-Actor") String actor) {
        try {
            DnaProposal proposal = proposalService.withdraw(id, actor);
            return ResponseEntity.ok(proposal);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/amend")
    public ResponseEntity<?> amendProposal(@PathVariable String id, @RequestBody Map<String, String> body,
                                            @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaProposal proposal = proposalService.amend(id, body.get("payload"), actor);
            return ResponseEntity.ok(proposal);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
