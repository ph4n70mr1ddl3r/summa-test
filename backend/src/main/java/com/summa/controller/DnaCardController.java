package com.summa.controller;

import com.summa.service.DnaCardService;
import com.summa.model.DnaCard;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dna/cards")
public class DnaCardController {
    private final DnaCardService cardService;
    private final AuditService auditService;

    public DnaCardController(DnaCardService cardService, AuditService auditService) {
        this.cardService = cardService;
        this.auditService = auditService;
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
        return cardService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createCard(@RequestBody Map<String, String> body,
                                         @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaCard card = cardService.create(
                body.get("id"),
                body.get("domainId"),
                body.get("title"),
                body.get("definitionMd"),
                body.get("provenance"),
                actor
            );
            return ResponseEntity.ok(card);
        } catch (Exception e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.badRequest().body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/drafts")
    public ResponseEntity<?> createDraft(@RequestBody Map<String, String> body,
                                           @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaCard card = cardService.createDraft(
                body.get("id"),
                body.get("domainId"),
                body.get("title"),
                body.get("definitionMd"),
                body.get("provenance"),
                actor
            );
            return ResponseEntity.ok(card);
        } catch (Exception e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.badRequest().body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateCard(@PathVariable String id, @RequestBody Map<String, String> body,
                                         @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
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
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        } catch (IllegalStateException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<?> retireCard(@PathVariable String id,
                                         @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaCard card = cardService.retire(id, actor);
            return ResponseEntity.ok(card);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
