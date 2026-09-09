package com.summa.controller;

import com.summa.service.DnaCardService;
import com.summa.model.DnaCard;
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
@RequestMapping("/dna/cards")
public class DnaCardController {
    private final DnaCardService cardService;
    private final AuditService auditService;
    private final WriteGate writeGate;

    public DnaCardController(DnaCardService cardService, AuditService auditService, WriteGate writeGate) {
        this.cardService = cardService;
        this.auditService = auditService;
        this.writeGate = writeGate;
    }

    @GetMapping
    public ResponseEntity<List<DnaCard>> listCards(
            @RequestParam(required = false) String domainId) {
        if (domainId != null) {
            return ResponseEntity.ok(cardService.findByDomain(domainId));
        }
        return ResponseEntity.ok(cardService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCard(@PathVariable String id) {
        Optional<DnaCard> entOpt = cardService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Card not found: " + id);
    }

    @PostMapping
    public ResponseEntity<?> createCard(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            if (body.get("title") == null || body.get("title").isBlank()) {
                throw new IllegalArgumentException("title is required");
            }
            String generatedId = UUID.randomUUID().toString();
            DnaCard card = cardService.create(
                generatedId,
                body.get("domainId"),
                body.get("title"),
                body.get("definitionMd"),
                body.get("provenance"),
                actor
            );
            return ResponseEntity.ok(card);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/drafts")
    public ResponseEntity<?> createDraft(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            if (body.get("title") == null || body.get("title").isBlank()) {
                throw new IllegalArgumentException("title is required");
            }
            String generatedId = UUID.randomUUID().toString();
            DnaCard card = cardService.createDraft(
                generatedId,
                body.get("domainId"),
                body.get("title"),
                body.get("definitionMd"),
                body.get("provenance"),
                actor
            );
            return ResponseEntity.ok(card);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateCard(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaCard card = cardService.update(
                id,
                body.get("title"),
                body.get("definitionMd"),
                body.get("provenance"),
                actor
            );
            return ResponseEntity.ok(card);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<?> retireCard(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaCard card = cardService.retire(id, actor);
            return ResponseEntity.ok(card);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }
}
