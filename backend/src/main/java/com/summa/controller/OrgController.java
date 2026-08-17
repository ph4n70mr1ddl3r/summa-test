package com.summa.controller;

import com.summa.service.OrgService;
import com.summa.service.AuditService;
import com.summa.service.MemberService;
import com.summa.model.Human;
import com.summa.model.AuditEvent;
import com.summa.model.Agent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/org")
public class OrgController {
    private final OrgService orgService;
    private final AuditService auditService;
    private final MemberService memberService;

    public OrgController(OrgService orgService, AuditService auditService, MemberService memberService) {
        this.orgService = orgService;
        this.auditService = auditService;
        this.memberService = memberService;
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<?> bootstrap(@RequestBody Map<String, String> body) {
        try {
            Human human = orgService.bootstrap(
                body.get("name"),
                body.get("email"),
                body.get("rbac")
            );
            return ResponseEntity.ok(Map.of("id", human.getId(), "email", human.getEmail(), "rbac", human.getRbac()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/humans")
    public ResponseEntity<List<Human>> listHumans(@RequestParam(defaultValue = "true") boolean active) {
        List<Human> humans = active ? orgService.findAllActiveHumans() : orgService.findAllHumans();
        return ResponseEntity.ok(humans);
    }

    @GetMapping("/humans/{id}")
    public ResponseEntity<?> getHuman(@PathVariable String id) {
        return orgService.findHuman(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/humans/{id}/rbac")
    public ResponseEntity<?> updateRbac(@PathVariable String id, @RequestBody Map<String, String> body,
                                          @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Human human = orgService.updateRbac(id, body.get("rbac"), actor);
            return ResponseEntity.ok(human);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        } catch (IllegalStateException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PutMapping("/humans/{id}/deputy")
    public ResponseEntity<?> setDeputy(@PathVariable String id, @RequestBody Map<String, String> body,
                                        @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Human human = orgService.setDeputy(id, body.get("deputyMemberId"), actor);
            return ResponseEntity.ok(human);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/humans/{id}/offboard")
    public ResponseEntity<?> offboard(@PathVariable String id,
                                       @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Human human = orgService.offboard(id, actor);
            return ResponseEntity.ok(human);
        } catch (IllegalStateException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/humans/{id}/erasure")
    public ResponseEntity<?> erasure(@PathVariable String id,
                                      @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        // API-005: admin, audited, honors data_holds (STG-030..034)
        try {
            orgService.findHuman(id).orElseThrow(() -> new IllegalArgumentException("Human not found: " + id));
            auditService.log(actor, "ERASURE_REQUEST", "human", id, null);
            return ResponseEntity.ok(Map.of("status", "erasure_requested", "id", id));
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @GetMapping("/members")
    public ResponseEntity<?> listMembers() {
        // API-004: combined view of humans + active agents
        List<Human> humans = orgService.findAllActiveHumans();
        List<Agent> agents = memberService.findAllActiveAgents();

        List<Map<String, Object>> members = new ArrayList<>();
        for (Human h : humans) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", h.getId());
            m.put("kind", "human");
            m.put("name", h.getName());
            m.put("rbac", h.getRbac());
            m.put("active", h.isActive());
            members.add(m);
        }
        for (Agent a : agents) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("kind", "agent");
            m.put("name", a.getName());
            m.put("class", a.getAgentClass());
            m.put("status", a.getStatus());
            members.add(m);
        }
        return ResponseEntity.ok(Map.of("members", members, "total", members.size()));
    }

    @GetMapping("/lineage")
    public ResponseEntity<?> lineage(@RequestParam String memberId) {
        // API-004: full lineage graph from any member
        List<String> lineage = new ArrayList<>();
        String[] holder = new String[]{memberId};
        while (holder[0] != null && lineage.size() < 20) {
            lineage.add(holder[0]);
            final String nextId = holder[0];
            memberService.findAgent(nextId).ifPresent(a -> holder[0] = a.getSpawnedBy());
        }
        return ResponseEntity.ok(Map.of("memberId", memberId, "lineage", lineage));
    }

    @GetMapping("/audit")
    public ResponseEntity<List<AuditEvent>> getAuditLog(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) String objectId) {
        if (objectType != null && objectId != null) {
            return ResponseEntity.ok(orgService.getAuditLogForEntity(objectType, objectId));
        }
        return ResponseEntity.ok(orgService.getAuditLog(limit));
    }
}
