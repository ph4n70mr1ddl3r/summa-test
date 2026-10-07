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
import java.util.LinkedHashMap;
import java.util.Map;
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
            "spawn-budget-window-days",
            "node-affinity-starvation-hours"
    );

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
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!requireAdmin(actor)) return ControllerResponses.gate(auditService, actor, "Admin access required to view governance policies");
        return ResponseEntity.ok(governanceService.getAllSettings());
    }

    @GetMapping("/quotas")
    public ResponseEntity<Map<String, Object>> getQuotas() {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!requireAdmin(actor)) return ControllerResponses.gate(auditService, actor, "Admin access required to view governance quotas");
        Map<String, Object> all = governanceService.getAllSettings();
        Map<String, Object> quotas = new LinkedHashMap<>();
        for (String key : QUOTA_KEYS) {
            quotas.put(key, all.get(key));
        }
        return ResponseEntity.ok(quotas);
    }

    @GetMapping("/spend")
    public ResponseEntity<Map<String, Object>> getSpend() {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!requireAdmin(actor)) return ControllerResponses.gate(auditService, actor, "Admin access required to view spend data");
        // Delegate defaults to GovernanceService to avoid divergence
        return ResponseEntity.ok(governanceService.getSpendView());
    }

    private static ResponseEntity<Map<String, Object>> validateNumberOrBoolean(
            String key, Object value, String label, AuditService auditService) {
        if (value instanceof Boolean) {
            return null;
        } else if (value instanceof Number) {
            return null;
        } else if (value instanceof String) {
            try {
                double parsed = Double.parseDouble((String) value);
                if (Double.isNaN(parsed) || Double.isInfinite(parsed) || parsed < 0) {
                    return ControllerResponses.validation(auditService, label + " value for '" + key + "' must be a non-negative number, not '" + value + "'");
                }
            } catch (NumberFormatException e) {
                return ControllerResponses.validation(auditService, label + " value for '" + key + "' must be a number, not '" + value + "'");
            }
            return null;
        } else {
            return ControllerResponses.validation(auditService, label + " value for '" + key + "' must be a number, boolean, or string, got: " + (value != null ? value.getClass().getSimpleName() : "null"));
        }
    }

    @PutMapping("/policies")
    public ResponseEntity<?> updatePolicy(@RequestBody Map<String, Object> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!requireAdmin(actor)) return ControllerResponses.gate(auditService, actor, "Admin access required to update governance policies");
        for (String key : body.keySet()) {
            if (!POLICY_KEYS.contains(key)) {
                return ControllerResponses.validation(auditService, "Unknown policy key: " + key);
            }
            ResponseEntity<Map<String, Object>> invalid = validateNumberOrBoolean(key, body.get(key), "Policy", auditService);
            if (invalid != null) return invalid;
        }
        governanceService.setSettingsBulk(body, actor);
        return ResponseEntity.ok(governanceService.getAllSettings());
    }

    @PutMapping("/quotas")
    public ResponseEntity<?> updateQuotas(@RequestBody Map<String, Object> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!requireAdmin(actor)) return ControllerResponses.gate(auditService, actor, "Admin access required to update governance quotas");
        for (String key : body.keySet()) {
            if (!QUOTA_KEYS.contains(key)) {
                return ControllerResponses.validation(auditService, "Unknown quota key: " + key);
            }
            ResponseEntity<Map<String, Object>> invalid = validateNumberOrBoolean(key, body.get(key), "Quota", auditService);
            if (invalid != null) return invalid;
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
        if (!requireAdmin(actor)) return ControllerResponses.gate(auditService, actor, "Admin access required to acknowledge spend overruns");
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
        Optional<Human> actorOpt = memberService.findHuman(actor);
        if (actorOpt.isPresent()) {
            return actorOpt.get().isActive()
                    && RbacRole.ADMIN.getValue().equals(actorOpt.get().getRbac());
        }
        // Check if actor is an admin agent (agents don't have RBAC, so only humans can be admins)
        return false;
    }
}
