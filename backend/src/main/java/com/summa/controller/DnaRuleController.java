package com.summa.controller;

import com.summa.service.DnaRuleService;
import com.summa.model.DnaRule;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dna/rules")
public class DnaRuleController {
    private final DnaRuleService ruleService;

    public DnaRuleController(DnaRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public ResponseEntity<List<DnaRule>> listRules(
            @RequestParam(required = false) String domainId) {
        if (domainId != null) {
            return ResponseEntity.ok(ruleService.findByDomain(domainId));
        }
        return ResponseEntity.ok(ruleService.findAllActiveWindowed(Instant.now()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRule(@PathVariable String id) {
        return ruleService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createRule(@RequestBody Map<String, String> body,
                                         @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Instant effectiveFrom = body.containsKey("effectiveFrom") ? 
                Instant.parse(body.get("effectiveFrom")) : Instant.now();
            Instant effectiveTo = body.containsKey("effectiveTo") ? 
                Instant.parse(body.get("effectiveTo")) : null;
            
            DnaRule rule = ruleService.create(
                body.get("id"),
                body.get("domainId"),
                body.get("statementMd"),
                body.get("machineHint"),
                effectiveFrom,
                effectiveTo,
                body.get("supersedesId"),
                actor
            );
            return ResponseEntity.ok(rule);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateRule(@PathVariable String id, @RequestBody Map<String, String> body,
                                         @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Instant effectiveTo = body.containsKey("effectiveTo") ? 
                Instant.parse(body.get("effectiveTo")) : null;
            
            DnaRule rule = ruleService.update(
                id,
                body.get("statementMd"),
                body.get("machineHint"),
                effectiveTo,
                actor
            );
            return ResponseEntity.ok(rule);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/supersede/{supersedesId}")
    public ResponseEntity<?> supersede(@PathVariable String id, @PathVariable String supersedesId,
                                        @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaRule rule = ruleService.supersede(id, supersedesId, actor);
            return ResponseEntity.ok(rule);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
