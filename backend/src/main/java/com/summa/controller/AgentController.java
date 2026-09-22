package com.summa.controller;

import com.summa.service.AgentService;
import com.summa.model.Agent;
import com.summa.service.AuditService;
import com.summa.service.AskService;
import com.summa.model.Ask;
import com.summa.security.WriteGate;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.enums.RbacRole;
import com.summa.service.OffboardingWalkService;
import com.summa.model.RoleTemplate;
import com.summa.repository.RoleTemplateRepository;
import com.summa.exception.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@RestController
@RequestMapping("/agents")
public class AgentController {
    private final AgentService agentService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final AskService askService;
    private final RoleTemplateRepository roleTemplateRepository;
    private final ObjectMapper objectMapper;

    public AgentController(AgentService agentService, AuditService auditService, WriteGate writeGate,
                           AskService askService, RoleTemplateRepository roleTemplateRepository, ObjectMapper objectMapper) {
        this.agentService = agentService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.askService = askService;
        this.roleTemplateRepository = roleTemplateRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<List<Agent>> listAgents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ownerId) {
        if (status != null) {
            return ResponseEntity.ok(agentService.findByStatus(status));
        }
        if (ownerId != null) {
            return ResponseEntity.ok(agentService.findByOwner(ownerId));
        }
        return ResponseEntity.ok(agentService.findAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAgent(@PathVariable String id) {
        Optional<Agent> entOpt = agentService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Agent not found: " + id);
    }

    @GetMapping("/{id}/lineage")
    public ResponseEntity<List<String>> getLineage(@PathVariable String id) {
        List<String> lineage = new ArrayList<>();
        String currentId = id;
        int depthCap = agentService.getDepthCap();
        while (currentId != null && lineage.size() < depthCap) {
            lineage.add(currentId);
            Optional<Agent> agentOpt = agentService.findById(currentId);
            if (agentOpt.isPresent()) {
                currentId = agentOpt.get().getSpawnedBy();
            } else {
                currentId = null;
            }
        }
        return ResponseEntity.ok(lineage);
    }

    @PostMapping("/{id}/deny")
    public ResponseEntity<?> deny(@PathVariable String id) {
        return lifecycleAction(id, actor -> agentService.deny(id, actor));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<?> suspend(@PathVariable String id) {
        return lifecycleAction(id, actor -> agentService.suspend(id, actor));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resume(@PathVariable String id) {
        return lifecycleAction(id, actor -> agentService.resume(id, actor));
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<?> retire(@PathVariable String id) {
        return lifecycleAction(id, actor -> agentService.retire(id, actor));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archive(@PathVariable String id) {
        return lifecycleAction(id, actor -> agentService.archive(id, actor));
    }

    private ResponseEntity<?> lifecycleAction(String id, Function<String, Agent> action) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Agent agent = action.apply(actor);
            return ResponseEntity.ok(agent);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/promote")
    public ResponseEntity<?> promote(@PathVariable String id, @RequestBody Map<String, String> body) {
        // API-033: files promotion ask for customRole hire per TPL-040..046
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Agent agent = agentService.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Agent not found: " + id));
            // TPL-040: Only customRole hires (template_id null) are eligible for promotion
            if (agent.getTemplateId() != null) {
                throw new IllegalStateException("Only customRole hires (no template) can be promoted");
            }
            // TPL-046: One live promotion ask per hire — refuse if already pending
            // Check asks TO the agent being promoted, not asks TO the actor
            List<Ask> pendingPromoAsks = askService.findByToAndStatusPending(id).stream()
                    .filter(a -> "promotion".equals(a.getKind()))
                    .toList();
            boolean hasPromoForAgent = false;
            for (Ask a : pendingPromoAsks) {
                try {
                    JsonNode node = objectMapper.readTree(a.getPayload());
                    JsonNode agentIdNode = node.get("agentId");
                    if (agentIdNode != null && id.equals(agentIdNode.asText())) {
                        hasPromoForAgent = true;
                        break;
                    }
                } catch (Exception e) {
                    auditService.logSystem("PROMOTE_PARSE_FAIL", "agent", id,
                        String.format("{\"error\":\"%s\"}", e.getMessage()));
                }
            }
            if (hasPromoForAgent) {
                throw new IllegalStateException("A promotion ask already exists for this hire");
            }
            String placement = body.get("placement");
            if (placement == null || placement.isBlank()) {
                throw new IllegalArgumentException("placement is required (template name or 'new:<name>:<version>')");
            }
            // TPL-040: Validate placement references an active template (or is a new role designation)
            if (!placement.startsWith("new:")) {
                Optional<RoleTemplate> tmplOpt = roleTemplateRepository.findByName(placement);
                if (tmplOpt.isEmpty() || !"active".equals(tmplOpt.get().getStatus())) {
                    throw new IllegalArgumentException("Placement template not found or not active: " + placement);
                }
            }
            // TPL-040: Snapshot identity files and effective scopes at creation
            Map<String, String> snapshotMap = new HashMap<>();
            snapshotMap.put("agentId", id);
            snapshotMap.put("agentName", agent.getName());
            snapshotMap.put("class", agent.getAgentClass());
            snapshotMap.put("placement", placement);
            snapshotMap.put("scopes", agent.getTemplateId() != null ? "templated" : "custom");
            String snapshotPayload;
            try {
                snapshotPayload = objectMapper.writeValueAsString(snapshotMap);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize promotion snapshot: " + e.getMessage());
            }
            askService.create("promotion", actor, OffboardingWalkService.ADMIN_BROADCAST,
                snapshotPayload, "standard", "deny", 1,
                Instant.now().plusSeconds(7 * 86400L), null, null);
            auditService.log(actor, "PROMOTE_REQUEST", "agent", id, snapshotPayload);
            return ResponseEntity.ok(Map.of("message", "Promotion ask filed", "agentId", id, "placement", placement));
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }
}
