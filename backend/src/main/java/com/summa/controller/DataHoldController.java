package com.summa.controller;

import com.summa.service.DataHoldService;
import com.summa.model.DataHold;
import com.summa.service.AuditService;
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
            String kind = body.get("kind");
            if (kind == null || kind.isBlank()) {
                throw new IllegalArgumentException("kind is required");
            }
            String subjectId = body.get("subjectId");
            if (subjectId == null || subjectId.isBlank()) {
                throw new IllegalArgumentException("subjectId is required");
            }
            String reasonMd = body.get("reasonMd");
            if (reasonMd == null || reasonMd.isBlank()) {
                throw new IllegalArgumentException("reasonMd is required");
            }
            DataHold hold = holdService.create(
                kind,
                subjectId,
                reasonMd,
                actor
            );
            return ResponseEntity.ok(hold);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
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
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }
}
