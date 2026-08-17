package com.summa.controller;

import com.summa.service.DNAReadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dna/search")
public class DnaSearchController {
    private final DNAReadService dnaReadService;

    public DnaSearchController(DNAReadService dnaReadService) {
        this.dnaReadService = dnaReadService;
    }

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam String q,
            @RequestParam(required = false) String domainId,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<Map<String, Object>> results = dnaReadService.search(q, domainId, limit);
            return ResponseEntity.ok(Map.of("results", results, "count", results.size()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("results", List.of(), "count", 0, "error", e.getMessage()));
        }
    }

    @GetMapping("/org-snapshot")
    public ResponseEntity<?> orgSnapshot(@RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        return ResponseEntity.ok(dnaReadService.getOrgSnapshot(actor));
    }

    @GetMapping("/review-queue")
    public ResponseEntity<?> reviewQueue(@RequestParam(required = false) String domainId) {
        return ResponseEntity.ok(dnaReadService.getReviewQueue(domainId));
    }

    @GetMapping("/domains")
    public ResponseEntity<?> listDomains() {
        return ResponseEntity.ok(dnaReadService.listDomains());
    }
}
