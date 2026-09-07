package com.summa.controller;

import com.summa.service.DnaGlossaryService;
import com.summa.model.DnaGlossary;
import com.summa.service.AuditService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/dna/glossary")
public class DnaGlossaryController {
    private final DnaGlossaryService glossaryService;
    private final AuditService auditService;
    private final WriteGate writeGate;

    public DnaGlossaryController(DnaGlossaryService glossaryService, AuditService auditService, WriteGate writeGate) {
        this.glossaryService = glossaryService;
        this.auditService = auditService;
        this.writeGate = writeGate;
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
    public ResponseEntity<?> createEntry(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            if (body.get("term") == null || body.get("term").isBlank()) {
                throw new IllegalArgumentException("term is required");
            }
            // Security: reject client-supplied IDs — always generate server-side
            String generatedId = UUID.randomUUID().toString();
            DnaGlossary entry = glossaryService.create(
                generatedId,
                body.get("domainId"),
                body.get("term"),
                body.get("definition"),
                body.get("aliases"),
                actor
            );
            return ResponseEntity.ok(entry);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateEntry(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaGlossary entry = glossaryService.update(
                id,
                body.get("definition"),
                body.get("aliases"),
                actor
            );
            return ResponseEntity.ok(entry);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<?> retireEntry(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaGlossary entry = glossaryService.retire(id, actor);
            return ResponseEntity.ok(entry);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }
}
