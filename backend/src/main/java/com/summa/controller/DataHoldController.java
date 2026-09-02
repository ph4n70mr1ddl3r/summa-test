package com.summa.controller;

import com.summa.service.DataHoldService;
import com.summa.model.DataHold;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/governance/holds")
public class DataHoldController {
    private final DataHoldService holdService;
    private final AuditService auditService;
    private final WriteGate writeGate;

    public DataHoldController(DataHoldService holdService, AuditService auditService, WriteGate writeGate) {
        this.holdService = holdService;
        this.auditService = auditService;
        this.writeGate = writeGate;
    }

    @GetMapping
    public ResponseEntity<List<DataHold>> listHolds() {
        return ResponseEntity.ok(holdService.findAllActive());
    }

    @PostMapping
    public ResponseEntity<?> createHold(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DataHold hold = holdService.create(
                body.get("kind"),
                body.get("subjectId"),
                body.get("reasonMd"),
                actor
            );
            return ResponseEntity.ok(hold);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "validation", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        } catch (IllegalStateException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<?> releaseHold(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            DataHold hold = holdService.release(id, actor);
            return ResponseEntity.ok(hold);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }
}
