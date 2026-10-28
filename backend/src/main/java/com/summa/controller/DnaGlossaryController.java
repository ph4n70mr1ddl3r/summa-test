package com.summa.controller;

import com.summa.service.DnaGlossaryService;
import com.summa.model.DnaGlossary;
import com.summa.model.DnaDomain;
import com.summa.util.DomainGuards;
import com.summa.service.AuditService;
import com.summa.exception.EntityNotFoundException;
import com.summa.service.DnaDomainService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.service.MemberService;
import com.summa.util.JsonHelpers;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/dna/glossary")
public class DnaGlossaryController {
    private final DnaGlossaryService glossaryService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final MemberService memberService;
    private final DnaDomainService domainService;

    public DnaGlossaryController(DnaGlossaryService glossaryService, AuditService auditService, WriteGate writeGate, MemberService memberService, DnaDomainService domainService) {
        this.glossaryService = glossaryService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.memberService = memberService;
        this.domainService = domainService;
    }

    @GetMapping
    public ResponseEntity<?> listEntries(
            @RequestParam(required = false) String domainId,
            @RequestParam(required = false) String scope) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
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
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        Optional<DnaGlossary> entOpt = glossaryService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Glossary entry not found: " + id);
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
            String rawDomainId = body.get("domainId");
            String domainIdClean = (rawDomainId != null && !rawDomainId.isBlank()) ? JsonHelpers.stripIdPrefix(rawDomainId) : null;
            DnaDomain domain = null;
            if (domainIdClean != null) {
                domain = domainService.findById(domainIdClean)
                    .orElseThrow(() -> new EntityNotFoundException("Domain not found: " + rawDomainId));
                DomainGuards.requireWritable(domain, domainIdClean);
            }
            DomainGuards.requireOwnerOrAdmin(memberService, domain, domainIdClean, actor);
            // Security: reject client-supplied IDs — always generate server-side
            String generatedId = UUID.randomUUID().toString();
            DnaGlossary entry = glossaryService.create(
                generatedId,
                domainIdClean,
                body.get("term"),
                body.get("definition"),
                body.get("aliases"),
                actor
            );
            return ResponseEntity.ok(entry);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        } catch (EntityNotFoundException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateEntry(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaGlossary existing = glossaryService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Glossary entry not found: " + id));
            DnaDomain domain = existing.getDomainId() != null ? domainService.findById(existing.getDomainId()).orElse(null) : null;
            DomainGuards.requireOwnerOrAdmin(memberService, domain, existing.getDomainId(), actor);
            DnaGlossary entry = glossaryService.update(
                id,
                body.get("definition"),
                body.get("aliases"),
                actor
            );
            return ResponseEntity.ok(entry);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        } catch (EntityNotFoundException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<?> retireEntry(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaGlossary existing = glossaryService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Glossary entry not found: " + id));
            DnaDomain domain = existing.getDomainId() != null ? domainService.findById(existing.getDomainId()).orElse(null) : null;
            DomainGuards.requireOwnerOrAdmin(memberService, domain, existing.getDomainId(), actor);
            DnaGlossary entry = glossaryService.retire(id, actor);
            return ResponseEntity.ok(entry);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        } catch (EntityNotFoundException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }
}
