package com.summa.controller;

import com.summa.service.DnaReadService;
import com.summa.security.RbacAuthorizationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dna/search")
public class DnaSearchController {
    private static final Logger log = LoggerFactory.getLogger(DnaSearchController.class);
    private final DnaReadService dnaReadService;

    public DnaSearchController(DnaReadService dnaReadService) {
        this.dnaReadService = dnaReadService;
    }

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam String q,
            @RequestParam(required = false) String domainId,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            if (q == null || q.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(Map.of("code", "validation", "message", "Query parameter 'q' is required"));
            }
            List<Map<String, Object>> results = dnaReadService.search(q, domainId, limit);
            return ResponseEntity.ok(Map.of("results", results, "count", results.size()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("code", "validation", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Search failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "internal", "message", "Internal server error"));
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
