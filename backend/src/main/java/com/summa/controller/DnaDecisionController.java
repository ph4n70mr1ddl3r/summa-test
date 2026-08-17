package com.summa.controller;

import com.summa.service.DnaDecisionService;
import com.summa.model.DnaDecision;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dna/decisions")
public class DnaDecisionController {
    private final DnaDecisionService decisionService;

    public DnaDecisionController(DnaDecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @GetMapping
    public ResponseEntity<List<DnaDecision>> listDecisions(
            @RequestParam(required = false) String domainId) {
        if (domainId != null) {
            return ResponseEntity.ok(decisionService.findByDomain(domainId));
        }
        return ResponseEntity.ok(decisionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDecision(@PathVariable String id) {
        return decisionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createDecision(@RequestBody Map<String, String> body,
                                             @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaDecision decision = decisionService.create(
                body.get("id"),
                body.get("domainId"),
                body.get("contextMd"),
                body.get("outcomeMd"),
                body.get("decidedBy"),
                body.get("provenance"),
                actor
            );
            return ResponseEntity.ok(decision);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
