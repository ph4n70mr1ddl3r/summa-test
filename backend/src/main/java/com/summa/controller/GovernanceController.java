package com.summa.controller;

import com.summa.service.GovernanceService;
import com.summa.service.SpendLedgerService;
import com.summa.model.SpendLedger;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import java.util.Map;

@RestController
@RequestMapping("/governance")
public class GovernanceController {
    private final GovernanceService governanceService;
    private final SpendLedgerService spendLedgerService;
    private final WriteGate writeGate;

    public GovernanceController(GovernanceService governanceService, SpendLedgerService spendLedgerService, WriteGate writeGate) {
        this.governanceService = governanceService;
        this.spendLedgerService = spendLedgerService;
        this.writeGate = writeGate;
    }

    @GetMapping("/policies")
    public ResponseEntity<Map<String, Object>> getPolicies() {
        return ResponseEntity.ok(governanceService.getAllSettings());
    }

    @GetMapping("/quotas")
    public ResponseEntity<Map<String, Object>> getQuotas() {
        Map<String, Object> all = governanceService.getAllSettings();
        Map<String, Object> quotas = new HashMap<>();
        quotas.put("spawn-ephemeral-default-ttl-hours", all.get("spawn-ephemeral-default-ttl-hours"));
        quotas.put("spawn-ephemeral-max-concurrent-per-spawner", all.get("spawn-ephemeral-max-concurrent-per-spawner"));
        quotas.put("spawn-org-wide-max-active-agents", all.get("spawn-org-wide-max-active-agents"));
        quotas.put("spawn-depth-cap", all.get("spawn-depth-cap"));
        quotas.put("asks-tier-critical-deadline-hours", all.get("asks-tier-critical-deadline-hours"));
        quotas.put("asks-tier-bulk-deadline-hours", all.get("asks-tier-bulk-deadline-hours"));
        quotas.put("asks-storm-collapse-window-hours", all.get("asks-storm-collapse-window-hours"));
        quotas.put("asks-rate-limit-per-source-per-hour", all.get("asks-rate-limit-per-source-per-hour"));
        return ResponseEntity.ok(quotas);
    }

    @GetMapping("/spend")
    public ResponseEntity<Map<String, Object>> getSpend() {
        // Delegate defaults to GovernanceService to avoid divergence (CFG-070)
        return ResponseEntity.ok(governanceService.getSpendView());
    }

    @PutMapping("/policies")
    public ResponseEntity<?> updatePolicy(@RequestBody Map<String, Object> body) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        body.forEach((key, value) -> governanceService.setSetting(key, value, actor));
        return ResponseEntity.ok(governanceService.getAllSettings());
    }

    @PutMapping("/quotas")
    public ResponseEntity<?> updateQuotas(@RequestBody Map<String, Object> body) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        body.forEach((key, value) -> governanceService.setSetting(key, value, actor));
        return ResponseEntity.ok(governanceService.getAllSettings());
    }

    @PostMapping("/spend/overruns/{id}/ack")
    public ResponseEntity<?> ackSpendOverrun(@PathVariable String id) {
        // API-051: admin; lifts the SPW-035 reserve gate
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            SpendLedger ledger = spendLedgerService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Spend ledger row not found: " + id));
            if (!ledger.getAcknowledged()) {
                spendLedgerService.acknowledge(id, actor);
            }
            return ResponseEntity.ok(Map.of("status", "overrun_acknowledged", "rowId", id,
                    "haltTripped", governanceService.isSpendHaltTripped()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage()));
        }
    }
}
