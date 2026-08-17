package com.summa.controller;

import com.summa.service.DnaCardService;
import com.summa.model.DnaCard;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dna/cards")
public class DnaCardController {
    private final DnaCardService cardService;

    public DnaCardController(DnaCardService cardService) {
        this.cardService = cardService;
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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
