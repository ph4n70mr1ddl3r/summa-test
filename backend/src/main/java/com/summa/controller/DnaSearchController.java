package com.summa.controller;

import com.summa.enums.RbacRole;
import com.summa.service.DnaReadService;
import com.summa.service.AuditService;
import com.summa.service.OrgService;
import com.summa.service.MemberService;
import com.summa.constants.Defaults;
import com.summa.security.RbacAuthorizationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/dna/search")
public class DnaSearchController {
    private static final Logger log = LoggerFactory.getLogger(DnaSearchController.class);
    private final DnaReadService dnaReadService;
    private final AuditService auditService;
    private final OrgService orgService;
    private final MemberService memberService;

    public DnaSearchController(DnaReadService dnaReadService, AuditService auditService, OrgService orgService,
                                MemberService memberService) {
        this.dnaReadService = dnaReadService;
        this.auditService = auditService;
        this.orgService = orgService;
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam String q,
            @RequestParam(required = false) String domainId,
            @RequestParam(defaultValue = "20") int limit) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        int cappedLimit = Math.min(Math.max(limit, 1), Defaults.MAX_DNA_SEARCH_LIMIT);
        try {
            List<Map<String, Object>> results = dnaReadService.search(q, domainId, cappedLimit);
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
        if (humanOpt.isEmpty() || !RbacRole.ADMIN.getValue().equals(humanOpt.get().getRbac())) {
            var audit = auditService.logSystem("REFUSAL", "dna_org_snapshot", actor, "Non-admin org snapshot access attempt");
            return ControllerResponses.gate(audit, "Admin access required for org snapshot");
        }
        return ResponseEntity.ok(dnaReadService.getOrgSnapshot());
    }

    @GetMapping("/domains")
    public ResponseEntity<?> listDomains() {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        return ResponseEntity.ok(dnaReadService.listDomains());
    }
}
