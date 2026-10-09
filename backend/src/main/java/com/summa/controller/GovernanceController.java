package com.summa.controller;

import com.summa.service.GovernanceService;
import com.summa.service.SpendLedgerService;
import com.summa.service.MemberService;
import com.summa.service.AuditService;
import com.summa.model.SpendLedger;
import com.summa.security.WriteGate;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.exception.EntityNotFoundException;
import com.summa.constants.Defaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import com.summa.util.JsonHelpers;

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
    public ResponseEntity<?> getPolicies() {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        if (!memberService.isAdmin(actor)) return ControllerResponses.gate(auditService, actor, "Admin access required to view governance policies");
        return ResponseEntity.ok(governanceService.getAllSettings());
    }

    @GetMapping("/quotas")
    public ResponseEntity<?> getQuotas() {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        if (!memberService.isAdmin(actor)) return ControllerResponses.gate(auditService, actor, "Admin access required to view governance quotas");
        Map<String, Object> all = governanceService.getAllSettings();
        Map<String, Object> quotas = new LinkedHashMap<>();
        for (String key : Defaults.QUOTA_KEYS) {
            quotas.put(key, all.get(key));
        }
        return ResponseEntity.ok(quotas);
    }

    @GetMapping("/spend")
    public ResponseEntity<?> getSpend() {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        if (!memberService.isAdmin(actor)) return ControllerResponses.gate(auditService, actor, "Admin access required to view spend data");
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
        if (!memberService.isAdmin(actor)) return ControllerResponses.gate(auditService, actor, "Admin access required to update governance policies");
        for (String key : body.keySet()) {
            if (!Defaults.POLICY_KEYS.contains(key)) {
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
        if (!memberService.isAdmin(actor)) return ControllerResponses.gate(auditService, actor, "Admin access required to update governance quotas");
        for (String key : body.keySet()) {
            if (!Defaults.QUOTA_KEYS.contains(key)) {
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
        if (!memberService.isAdmin(actor)) return ControllerResponses.gate(auditService, actor, "Admin access required to acknowledge spend overruns");
        SpendLedger ledger = spendLedgerService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Spend ledger row not found: " + id));
        // Verify the acknowledging admin is related to this spend entry (member or admin)
        String cleanActor = JsonHelpers.stripIdPrefix(actor);
        boolean isRelated = cleanActor.equals(ledger.getMemberId()) || memberService.isAdmin(cleanActor);
        if (!isRelated) {
            return ControllerResponses.gate(auditService, actor, "Admin must be related to this spend entry to acknowledge");
        }
        if (!Boolean.TRUE.equals(ledger.getAcknowledged())) {
            spendLedgerService.acknowledge(id, actor);
        }
        return ResponseEntity.ok(Map.of("status", "overrun_acknowledged", "rowId", id,
                "haltTripped", governanceService.isSpendHaltTripped()));
    }
}
