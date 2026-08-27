package com.summa.controller;

import com.summa.service.DnaDomainService;
import com.summa.model.DnaDomain;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dna/domains")
public class DnaDomainController {
    private final DnaDomainService domainService;
    private final AuditService auditService;
    private final WriteGate writeGate;

    public DnaDomainController(DnaDomainService domainService, AuditService auditService, WriteGate writeGate) {
        this.domainService = domainService;
        this.auditService = auditService;
        this.writeGate = writeGate;
    }

    @GetMapping
    public ResponseEntity<List<DnaDomain>> listDomains() {
        return ResponseEntity.ok(domainService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDomain(@PathVariable String id) {
        return domainService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createDomain(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            if (body.get("name") == null || body.get("name").isBlank()) {
                throw new IllegalArgumentException("name is required");
            }
            if (body.get("ownerHumanId") == null || body.get("ownerHumanId").isBlank()) {
                throw new IllegalArgumentException("ownerHumanId is required");
            }
            DnaDomain domain = domainService.create(
                body.get("id"),
                body.get("name"),
                body.get("ownerHumanId"),
                body.get("access"),
                body.get("store"),
                body.containsKey("reviewSlaDays") ? Integer.parseInt(body.get("reviewSlaDays")) : null,
                body.get("residency")
            );
            return ResponseEntity.ok(domain);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "validation", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archiveDomain(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaDomain domain = domainService.archive(id, actor);
            return ResponseEntity.ok(domain);
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

    @PostMapping("/{id}/rename")
    public ResponseEntity<?> renameDomain(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaDomain domain = domainService.rename(id, body.get("name"), actor);
            return ResponseEntity.ok(domain);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PatchMapping("/{id}/owner")
    public ResponseEntity<?> updateOwner(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaDomain domain = domainService.updateOwner(id, body.get("ownerHumanId"), actor);
            return ResponseEntity.ok(domain);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PatchMapping("/{id}/access")
    public ResponseEntity<?> updateAccess(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DnaDomain domain = domainService.updateAccess(id, body.get("access"), actor);
            return ResponseEntity.ok(domain);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    // API-023: topology ops — split and merge are stubs for Phase 6
    @PostMapping("/{id}/split")
    public ResponseEntity<?> splitDomain(@PathVariable String id, @RequestBody Map<String, String> body) {
        // DGV-010..013: governed split — stub for Phase 6
        return ResponseEntity.ok(Map.of("status", "not_implemented", "note", "Topology ops scheduled for Phase 6"));
    }

    @PostMapping("/{id}/merge")
    public ResponseEntity<?> mergeDomain(@PathVariable String id, @RequestBody Map<String, String> body) {
        // DGV-014: governed merge — stub for Phase 6
        return ResponseEntity.ok(Map.of("status", "not_implemented", "note", "Topology ops scheduled for Phase 6"));
    }
}
