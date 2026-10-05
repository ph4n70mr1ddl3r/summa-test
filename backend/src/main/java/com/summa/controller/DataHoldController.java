package com.summa.controller;

import com.summa.service.DataHoldService;
import com.summa.model.DataHold;
import com.summa.service.AuditService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.service.MemberService;
import com.summa.enums.DataHoldKind;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/governance/holds")
public class DataHoldController {
    private final DataHoldService holdService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final MemberService memberService;

    public DataHoldController(DataHoldService holdService, AuditService auditService, WriteGate writeGate, MemberService memberService) {
        this.holdService = holdService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<?> listHolds() {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        if (!"system".equals(actor)) {
            Optional<com.summa.model.Human> humanOpt = memberService.findHuman(actor);
            Optional<com.summa.model.Agent> agentOpt = memberService.findAgent(actor);
            if (!(humanOpt.isPresent() || agentOpt.isPresent())) {
                return ControllerResponses.gate(auditService, actor, "Authentication required");
            }
        }
        return ResponseEntity.ok(holdService.findAllActive());
    }

    @PostMapping
    public ResponseEntity<?> createHold(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Data hold creation requires admin role");
        }
        try {
            String kind = body.get("kind");
            if (kind == null || kind.isBlank()) {
                throw new IllegalArgumentException("kind is required");
            }
            // Validate kind against enum
            DataHoldKind holdKind = DataHoldKind.requireFromValue(kind);
            String subjectId = body.get("subjectId");
            if (subjectId == null || subjectId.isBlank()) {
                throw new IllegalArgumentException("subjectId is required");
            }
            String reasonMd = body.get("reasonMd");
            if (reasonMd == null || reasonMd.isBlank()) {
                throw new IllegalArgumentException("reasonMd is required");
            }
            DataHold hold = holdService.create(
                holdKind.getValue(),
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
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Data hold release requires admin role");
        }
        try {
            DataHold hold = holdService.release(id, actor);
            return ResponseEntity.ok(hold);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }
}
