package com.summa.controller;

import com.summa.service.DnaGlossaryService;
import com.summa.model.DnaGlossary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dna/glossary")
public class DnaGlossaryController {
    private final DnaGlossaryService glossaryService;

    public DnaGlossaryController(DnaGlossaryService glossaryService) {
        this.glossaryService = glossaryService;
    }

    @GetMapping
    public ResponseEntity<List<DnaGlossary>> listEntries(
            @RequestParam(required = false) String domainId,
            @RequestParam(required = false) String scope) {
        if (scope != null) {
            return ResponseEntity.ok(glossaryService.findByScope(scope));
        }
        if (domainId != null) {
            return ResponseEntity.ok(glossaryService.findByDomain(domainId));
        }
        return ResponseEntity.ok(glossaryService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEntry(@PathVariable String id) {
        return glossaryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createEntry(@RequestBody Map<String, String> body,
                                          @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaGlossary entry = glossaryService.create(
                body.get("id"),
                body.get("domainId"),
                body.get("term"),
                body.get("definition"),
                body.get("aliases"),
                actor
            );
            return ResponseEntity.ok(entry);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateEntry(@PathVariable String id, @RequestBody Map<String, String> body,
                                          @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaGlossary entry = glossaryService.update(
                id,
                body.get("definition"),
                body.get("aliases"),
                actor
            );
            return ResponseEntity.ok(entry);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<?> retireEntry(@PathVariable String id,
                                          @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaGlossary entry = glossaryService.retire(id, actor);
            return ResponseEntity.ok(entry);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
