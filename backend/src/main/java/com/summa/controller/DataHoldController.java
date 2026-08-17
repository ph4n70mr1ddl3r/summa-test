package com.summa.controller;

import com.summa.service.DataHoldService;
import com.summa.model.DataHold;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/governance/holds")
public class DataHoldController {
    private final DataHoldService holdService;
    private final AuditService auditService;

    public DataHoldController(DataHoldService holdService, AuditService auditService) {
        this.holdService = holdService;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<DataHold>> listHolds() {
        return ResponseEntity.ok(holdService.findAllActive());
    }

    @PostMapping
    public ResponseEntity<?> createHold(@RequestBody Map<String, String> body,
                                         @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DataHold hold = holdService.create(
                body.get("kind"),
                body.get("subjectId"),
                body.get("reasonMd"),
                actor
            );
            return ResponseEntity.ok(hold);
        } catch (Exception e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.badRequest().body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<?> releaseHold(@PathVariable String id,
                                           @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DataHold hold = holdService.release(id, actor);
            return ResponseEntity.ok(hold);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
