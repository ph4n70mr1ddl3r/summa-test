package com.summa.controller;

import com.summa.service.GovernanceService;
import com.summa.service.SpendLedgerService;
import com.summa.service.MemberService;
import com.summa.service.AuditService;
import com.summa.model.SpendLedger;
import com.summa.model.Human;
import com.summa.security.WriteGate;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.enums.RbacRole;
import com.summa.exception.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/governance")
public class GovernanceController {
    private final GovernanceService governanceService;
    private final SpendLedgerService spendLedgerService;
    private final MemberService memberService;
    private final WriteGate writeGate;
    private final AuditService auditService;

    public GovernanceController(GovernanceService governanceService, SpendLedgerService spendLedgerService,
                                  MemberService memberService, WriteGate writeGate, AuditService auditService) {
        this.governanceService = governanceService;
        this.spendLedgerService = spendLedgerService;
        this.memberService = memberService;
        this.writeGate = writeGate;
        this.auditService = auditService;
    }

    @GetMapping("/policies")
    public ResponseEntity<Map<String, Object>> getPolicies() {
        return ResponseEntity.ok(governanceService.getAllSettings());
    }

    @GetMapping("/quotas")
    public ResponseEntity<Map<String, Object>> getQuotas() {
        Map<String, Object> all = governanceService.getAllSettings();
        Map<String, Object> quotas = new HashMap<>();
        for (String key : QUOTA_KEYS) {
            quotas.put(key, all.get(key));
        }
        return ResponseEntity.ok(quotas);
    }

    @GetMapping("/spend")
    public ResponseEntity<Map<String, Object>> getSpend() {
        // Delegate defaults to GovernanceService to avoid divergence
        return ResponseEntity.ok(governanceService.getSpendView());
    }

    private static final Set<String> POLICY_KEYS = Set.of(
            "asks-tier-critical-deadline-hours",
            "asks-tier-standard-deadline-hours",
            "asks-tier-bulk-deadline-hours",
            "asks-storm-collapse-window-hours",
            "asks-rate-limit-per-source-per-hour",
            "summa.dna.default-review-sla-days",
            "spend-org-ceiling",
            "spend-critical-floor-percent",
            "spend-evaluation-window-days"
    );

    private static final Set<String> QUOTA_KEYS = Set.of(
            "spawn-ephemeral-default-ttl-hours",
            "spawn-ephemeral-max-concurrent-per-spawner",
            "spawn-org-wide-max-active-agents",
            "spawn-depth-cap",
            "spawn-budget-window-days"
    );

    @PutMapping("/policies")
    public ResponseEntity<?> updatePolicy(@RequestBody Map<String, Object> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!requireAdmin(actor)) return ControllerResponses.gate(auditService, "Admin access required to update governance policies");
        for (String key : body.keySet()) {
            if (!POLICY_KEYS.contains(key)) {
                return ControllerResponses.validation(auditService, "Unknown policy key: " + key);
            }
            Object value = body.get(key);
            if (value instanceof Number) {
                // Numbers are accepted as-is
            } else if (value instanceof Boolean) {
                // Booleans are accepted as-is
            } else if (value instanceof String) {
                // Strings must parse as a number (integer or float) — reject freeform text
                try {
                    Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    return ControllerResponses.validation(auditService, "Policy value for '" + key + "' must be a number, not '" + value + "'");
                }
            } else {
                return ControllerResponses.validation(auditService, "Policy value for '" + key + "' must be a number, string, or boolean");
            }
        }
        governanceService.setSettingsBulk(body, actor);
        return ResponseEntity.ok(governanceService.getAllSettings());
    }

    @PutMapping("/quotas")
    public ResponseEntity<?> updateQuotas(@RequestBody Map<String, Object> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!requireAdmin(actor)) return ControllerResponses.gate(auditService, "Admin access required to update governance quotas");
        for (String key : body.keySet()) {
            if (!QUOTA_KEYS.contains(key)) {
                return ControllerResponses.validation(auditService, "Unknown quota key: " + key);
            }
            Object value = body.get(key);
            if (value instanceof Number) {
                // Numbers are accepted as-is
            } else if (value instanceof Boolean) {
                // Booleans are accepted as-is
            } else if (value instanceof String) {
                try {
                    Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    return ControllerResponses.validation(auditService, "Quota value for '" + key + "' must be a number, not '" + value + "'");
                }
            } else {
                return ControllerResponses.validation(auditService, "Quota value for '" + key + "' must be a number, string, or boolean");
            }
        }
        governanceService.setSettingsBulk(body, actor);
        return ResponseEntity.ok(governanceService.getAllSettings());
    }

    @PostMapping("/spend/overruns/{id}/ack")
    public ResponseEntity<?> ackSpendOverrun(@PathVariable String id) {
        // API-051: admin; lifts the SPW-035 reserve gate
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!requireAdmin(actor)) return ControllerResponses.gate(auditService, "Admin access required to acknowledge spend overruns");
        try {
            SpendLedger ledger = spendLedgerService.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Spend ledger row not found: " + id));
            if (!Boolean.TRUE.equals(ledger.getAcknowledged())) {
                spendLedgerService.acknowledge(id, actor);
            }
            return ResponseEntity.ok(Map.of("status", "overrun_acknowledged", "rowId", id,
                    "haltTripped", governanceService.isSpendHaltTripped()));
        } catch (EntityNotFoundException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }

    private boolean requireAdmin(String actor) {
        if (Boolean.TRUE.equals(RbacAuthorizationFilter.getNodeAuth())) return true;
        Optional<Human> actorOpt = memberService.findHuman(actor);
        return actorOpt.isPresent() && RbacRole.ADMIN.getValue().equals(actorOpt.get().getRbac());
    }
}
