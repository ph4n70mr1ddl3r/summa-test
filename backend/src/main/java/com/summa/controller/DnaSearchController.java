package com.summa.controller;

import com.summa.service.DnaReadService;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dna/search")
public class DnaSearchController {
    private final DnaReadService dnaReadService;
    private final AuditService auditService;

    public DnaSearchController(DnaReadService dnaReadService, AuditService auditService) {
        this.dnaReadService = dnaReadService;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam String q,
            @RequestParam(required = false) String domainId,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            if (q == null || q.isBlank()) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(Map.of("code", "validation", "message", "Query parameter 'q' is required"));
            }
            int safeLimit = Math.min(Math.max(limit, 1), 100);
            List<Map<String, Object>> results = dnaReadService.search(q, domainId, safeLimit);
            return ResponseEntity.ok(Map.of("results", results, "count", results.size()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("code", "validation", "message", e.getMessage()));
        } catch (Exception e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "internal", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @GetMapping("/org-snapshot")
    public ResponseEntity<?> orgSnapshot() {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        return ResponseEntity.ok(dnaReadService.getOrgSnapshot(actor));
    }

    @GetMapping("/domains")
    public ResponseEntity<?> listDomains() {
        return ResponseEntity.ok(dnaReadService.listDomains());
    }
}
