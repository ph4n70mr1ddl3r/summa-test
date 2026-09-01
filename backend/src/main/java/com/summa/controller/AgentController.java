package com.summa.controller;

import com.summa.service.AgentService;
import com.summa.model.Agent;
import com.summa.service.AuditService;
import com.summa.service.AskService;
import com.summa.repository.AskRepository;
import com.summa.model.Ask;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import com.summa.service.OffboardingWalkService;

@RestController
@RequestMapping("/agents")
public class AgentController {
    private final AgentService agentService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final AskService askService;
    private final AskRepository askRepository;

    public AgentController(AgentService agentService, AuditService auditService, WriteGate writeGate,
                           AskService askService, AskRepository askRepository) {
        this.agentService = agentService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.askService = askService;
        this.askRepository = askRepository;
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
        return agentService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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

    @PostMapping("/{id}/suspend")
    public ResponseEntity<?> suspend(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Agent agent = agentService.suspend(id, actor);
            return ResponseEntity.ok(agent);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resume(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Agent agent = agentService.resume(id, actor);
            return ResponseEntity.ok(agent);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<?> retire(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Agent agent = agentService.retire(id, actor);
            return ResponseEntity.ok(agent);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archive(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Agent agent = agentService.archive(id, actor);
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
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            com.summa.model.Agent agent = agentService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));
            // TPL-040: Only customRole hires (template_id null) are eligible for promotion
            if (agent.getTemplateId() != null) {
                throw new IllegalStateException("Only customRole hires (no template) can be promoted");
            }
            // TPL-046: One live promotion ask per hire — refuse if already pending
            // Check asks TO the agent being promoted, not asks TO the actor
            List<com.summa.model.Ask> pendingPromoAsks = askRepository.findByToAndStatusPending(id).stream()
                    .filter(a -> "promotion".equals(a.getKind()))
                    .toList();
            boolean hasPromoForAgent = false;
            for (com.summa.model.Ask a : pendingPromoAsks) {
                try {
                    com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(a.getPayload());
                    if (id.equals(node.get("agentId").asText())) {
                        hasPromoForAgent = true;
                        break;
                    }
                } catch (Exception ignored) {}
            }
            if (hasPromoForAgent) {
                throw new IllegalStateException("A promotion ask already exists for this hire");
            }
            String placement = body.get("placement");
            if (placement == null || placement.isBlank()) {
                throw new IllegalArgumentException("placement is required (template name or 'new:<name>:<version>')");
            }
            // TPL-040: Snapshot identity files and effective scopes at creation
            String snapshotPayload = String.format(
                "{\"agentId\":\"%s\",\"agentName\":\"%s\",\"class\":\"%s\",\"placement\":\"%s\",\"scopes\":\"%s\"}",
                id, agent.getName(), agent.getAgentClass(), placement,
                agent.getTemplateId() != null ? "templated" : "custom");
            askService.create("promotion", actor, OffboardingWalkService.ADMIN_BROADCAST,
                snapshotPayload, "standard", "deny", 1,
                Instant.now().plusSeconds(7 * 86400L), null, null);
            auditService.log(actor, "PROMOTE_REQUEST", "agent", id,
                String.format("{\"placement\":\"%s\"}", placement));
            return ResponseEntity.ok(Map.of("message", "Promotion ask filed", "agentId", id, "placement", placement));
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }
}
