package com.summa.controller;

import com.summa.service.DnaReadService;
import com.summa.service.AuditService;
import com.summa.service.OrgService;
import com.summa.security.RbacAuthorizationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dna/search")
public class DnaSearchController {
    private static final Logger log = LoggerFactory.getLogger(DnaSearchController.class);
    private final DnaReadService dnaReadService;
    private final AuditService auditService;
    private final OrgService orgService;

    public DnaSearchController(DnaReadService dnaReadService, AuditService auditService, OrgService orgService) {
        this.dnaReadService = dnaReadService;
        this.auditService = auditService;
        this.orgService = orgService;
    }

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam String q,
            @RequestParam(required = false) String domainId,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            if (q == null || q.isBlank()) {
                return ControllerResponses.validation(auditService, "Query parameter 'q' is required");
            }
            List<Map<String, Object>> results = dnaReadService.search(q, domainId, limit);
            return ResponseEntity.ok(Map.of("results", results, "count", results.size()));
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (Exception e) {
            log.error("Search failed", e);
            return ControllerResponses.internalError(auditService, "Internal server error");
        }
    }

    @GetMapping("/org-snapshot")
    public ResponseEntity<?> orgSnapshot() {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        // Org snapshot exposes member identities and RBAC — restrict to admins only.
        var humanOpt = orgService.findHuman(actor);
        if (humanOpt.isEmpty() || !"admin".equals(humanOpt.get().getRbac())) {
            var audit = auditService.logSystem("REFUSAL", "dna_org_snapshot", actor, "Non-admin org snapshot access attempt");
            return ControllerResponses.gate(auditService, actor, "Admin access required for org snapshot");
        }
        return ResponseEntity.ok(dnaReadService.getOrgSnapshot());
    }

    @GetMapping("/domains")
    public ResponseEntity<?> listDomains() {
        return ResponseEntity.ok(dnaReadService.listDomains());
    }
}
