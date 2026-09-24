package com.summa.controller;

import com.summa.model.Agent;
import com.summa.model.AuditEvent;
import com.summa.model.DataHold;
import com.summa.model.Human;
import com.summa.enums.RbacRole;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.security.WriteGate;
import com.summa.service.AgentService;
import com.summa.service.AuditService;
import com.summa.service.DataHoldService;
import com.summa.service.MemberService;
import com.summa.service.OrgService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.summa.exception.EntityNotFoundException;

@RestController
@RequestMapping("/org")
public class OrgController {
    private final OrgService orgService;
    private final AuditService auditService;
    private final MemberService memberService;
    private final DataHoldService dataHoldService;
    private final WriteGate writeGate;
    private final AgentService agentService;

    public OrgController(OrgService orgService, AuditService auditService, MemberService memberService,
                         DataHoldService dataHoldService, WriteGate writeGate, AgentService agentService) {
        this.orgService = orgService;
        this.auditService = auditService;
        this.memberService = memberService;
        this.dataHoldService = dataHoldService;
        this.writeGate = writeGate;
        this.agentService = agentService;
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<?> bootstrap(@RequestBody Map<String, String> body) {
        // First bootstrap is special — it creates the initial admin without auth.
        // Subsequent calls are gated by the WriteGate which requires admin auth.
        boolean alreadyInitialized = orgService.isInitialized();
        if (!alreadyInitialized) {
            try {
                Human human = orgService.bootstrap(
                    body.get("name"),
                    body.get("email"),
                    body.get("rbac"),
                    body.get("password")
                );
                return ResponseEntity.ok(Map.of("id", human.getId(), "email", human.getEmail(), "rbac", human.getRbac()));
            } catch (IllegalStateException e) {
                return ControllerResponses.gate(auditService, e.getMessage());
            }
        }
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Bootstrap requires admin role when org is initialized");
        }
        try {
            Human human = orgService.bootstrap(
                body.get("name"),
                body.get("email"),
                body.get("rbac"),
                body.get("password")
            );
            return ResponseEntity.ok(Map.of("id", human.getId(), "email", human.getEmail(), "rbac", human.getRbac()));
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @GetMapping("/humans")
    public ResponseEntity<?> listHumans(@RequestParam(defaultValue = "true") boolean active) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        if (!orgService.isInitialized() || !memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Admin access required to list humans");
        }
        List<Human> humans = active ? orgService.findAllActiveHumans() : orgService.findAllHumans();
        return ResponseEntity.ok(humans);
    }

    @GetMapping("/humans/{id}")
    public ResponseEntity<?> getHuman(@PathVariable String id) {
        Optional<Human> humanOpt = orgService.findHuman(id);
        if (humanOpt.isPresent()) {
            return ResponseEntity.ok(humanOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Human not found: " + id);
    }

    @PutMapping("/humans/{id}/rbac")
    public ResponseEntity<?> updateRbac(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            String rbacValue = body.get("rbac");
            if (rbacValue == null || rbacValue.isBlank()) {
                throw new IllegalArgumentException("rbac is required");
            }
            boolean validRole = false;
            for (RbacRole role : RbacRole.values()) {
                if (role.getValue().equals(rbacValue)) { validRole = true; break; }
            }
            if (!validRole) {
                throw new IllegalArgumentException("Invalid rbac: " + rbacValue + ". Must be one of: admin, owner, member, viewer");
            }
            Human human = orgService.updateRbac(id, rbacValue, actor);
            return ResponseEntity.ok(human);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PutMapping("/humans/{id}/demote")
    public ResponseEntity<?> demote(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        String newRbac = body.get("rbac");
        if (newRbac == null || newRbac.isBlank()) {
            return ControllerResponses.validation(auditService, "rbac is required for demote");
        }
        try {
            Human human = orgService.demote(id, newRbac, actor);
            return ResponseEntity.ok(human);
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        }
    }

    @PutMapping("/humans/{id}/deputy")
    public ResponseEntity<?> setDeputy(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Human human = orgService.setDeputy(id, body.get("deputyMemberId"), actor);
            return ResponseEntity.ok(human);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/humans/{id}/offboard")
    public ResponseEntity<?> offboard(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        Optional<Human> actorOpt = orgService.findHuman(actor);
        if (actorOpt.isEmpty() || !RbacRole.ADMIN.getValue().equals(actorOpt.get().getRbac())) {
            return ControllerResponses.gate(auditService, actor, "Offboarding requires admin role");
        }
        try {
            Human human = orgService.offboard(id, actor);
            return ResponseEntity.ok(human);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/humans/{id}/erasure")
    public ResponseEntity<?> erasure(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Erasure requires admin role");
        }
        // API-005: admin, audited, honors data_holds (STG-030..034)
        try {
            Human human = orgService.findHuman(id).orElseThrow(() -> new EntityNotFoundException("Human not found: " + id));

            List<DataHold> holds = dataHoldService.findBySubject("human", id);
            if (!holds.isEmpty()) {
                return ControllerResponses.conflict(auditService, "Active data holds prevent erasure");
            }

            orgService.erasure(id, actor);
            return ResponseEntity.ok(Map.of("status", "erased", "id", id));
        } catch (EntityNotFoundException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }

    @GetMapping("/members")
    public ResponseEntity<?> listMembers() {
        // API-004: combined view of humans + active agents
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
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
        int depthCap = agentService.getDepthCap();
        List<String> lineage = new ArrayList<>();
        String[] holder = new String[]{memberId};
        while (holder[0] != null && lineage.size() < depthCap) {
            lineage.add(holder[0]);
            final String nextId = holder[0];
            Optional<Agent> agentOpt = memberService.findAgent(nextId);
            if (agentOpt.isPresent()) {
                holder[0] = agentOpt.get().getSpawnedBy();
            } else {
                // Apply same depth cap to human deputy chain to prevent infinite loops
                memberService.findHuman(nextId).ifPresent(h -> {
                    if (lineage.size() < depthCap) {
                        holder[0] = h.getDeputyMemberId();
                    }
                });
                // If neither agent nor human found, stop the chain
                if (holder[0] == null) {
                    break;
                }
            }
        }
        return ResponseEntity.ok(Map.of("memberId", memberId, "lineage", lineage));
    }

    @GetMapping("/audit")
    public ResponseEntity<?> getAuditLog(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) String objectId) {
        if (limit <= 0 || limit > 1000) {
            return ControllerResponses.validation(auditService, "limit must be between 1 and 1000");
        }
        if (objectType != null && objectId != null) {
            return ResponseEntity.ok(orgService.getAuditLogForEntity(objectType, objectId));
        }
        return ResponseEntity.ok(orgService.getAuditLog(limit));
    }
}
