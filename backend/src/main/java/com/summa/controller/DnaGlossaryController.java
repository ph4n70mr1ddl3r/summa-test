package com.summa.controller;

import com.summa.service.DnaGlossaryService;
import com.summa.model.DnaGlossary;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<?> createEntry(@RequestBody Map<String, String> body,
                                           @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
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
            AuditEvent audit = auditService.logSystem("REFUSAL", "validation", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateEntry(@PathVariable String id, @RequestBody Map<String, String> body,
                                           @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
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
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        } catch (IllegalStateException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<?> retireEntry(@PathVariable String id,
                                           @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaGlossary entry = glossaryService.retire(id, actor);
            return ResponseEntity.ok(entry);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }
}
