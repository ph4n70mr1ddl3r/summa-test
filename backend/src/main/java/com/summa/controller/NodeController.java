package com.summa.controller;

import com.summa.model.Node;
import com.summa.model.Run;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.security.WriteGate;
import com.summa.service.AuditService;
import com.summa.service.NodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/nodes")
public class NodeController {
    private final NodeService nodeService;
    private final AuditService auditService;
    private final WriteGate writeGate;

    public NodeController(NodeService nodeService, AuditService auditService, WriteGate writeGate) {
        this.nodeService = nodeService;
        this.auditService = auditService;
        this.writeGate = writeGate;
    }

    @GetMapping
    public ResponseEntity<List<Node>> listNodes() {
        return ResponseEntity.ok(nodeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getNode(@PathVariable String id) {
        Optional<Node> entOpt = nodeService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Node not found: " + id);
    }

    @PostMapping("/enroll")
    public ResponseEntity<?> enroll(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String kind = body.get("kind");
        String pubkey = body.get("pubkey");
        if (name == null || name.isBlank() || kind == null || kind.isBlank() || pubkey == null || pubkey.isBlank()) {
            return ControllerResponses.validation(auditService, "name, kind, and pubkey are required");
        }
        if (!pubkey.matches("[A-Za-z0-9+/=]{16,}")) {
            return ControllerResponses.validation(auditService, "pubkey must be a non-empty base64-like string (at least 16 chars)");
        }
        try {
            Node node = nodeService.enroll(name, kind, pubkey);
            return ResponseEntity.ok(Map.of(
                "id", node.getId(),
                "enrollmentToken", nodeService.generateEnrollmentToken(node.getId())
            ));
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<?> heartbeat(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Node node = nodeService.heartbeat(id, body.get("capabilities"));
            return ResponseEntity.ok(node);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/claims")
    public ResponseEntity<?> claimWorkspace(@PathVariable String id, @RequestBody Map<String, String> body) {
        // API-060: acquire or renew a workspace claim as epoch-fenced lease
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            String workspaceId = body.get("workspaceId");
            if (workspaceId == null || workspaceId.isBlank()) {
                throw new IllegalArgumentException("workspaceId is required");
            }
            String epochStr = body.get("epoch");
            int currentEpoch = epochStr != null ? Integer.parseInt(epochStr.trim()) : 0;
            Node node = nodeService.claimWorkspace(id, workspaceId, currentEpoch);
            return ResponseEntity.ok(node);
        } catch (NumberFormatException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/work/pull")
    public ResponseEntity<?> pullWork(@PathVariable String id) {
        // API-060: fetch queued runs for workspaces the node holds a live claim on
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            List<Run> runs = nodeService.pullWork(id);
            return ResponseEntity.ok(runs);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/runs/{runId}/report")
    public ResponseEntity<?> reportRun(@PathVariable String id, @PathVariable String runId,
                                        @RequestBody Map<String, String> body) {
        // API-060: land results, artifacts, and spend ledger lines
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            String result = body.get("result");
            String artifacts = body.get("artifacts");
            String costTokensStr = body.get("costTokens");
            long costTokens = 0L;
            if (costTokensStr != null && !costTokensStr.isBlank()) {
                costTokens = Long.parseLong(costTokensStr.trim());
                if (costTokens < 0) throw new IllegalArgumentException("costTokens must be non-negative");
            }
            String costUsdStr = body.get("costUsd");
            double costUsd = 0.0;
            if (costUsdStr != null && !costUsdStr.isBlank()) {
                costUsd = Double.parseDouble(costUsdStr.trim());
                if (costUsd < 0) throw new IllegalArgumentException("costUsd must be non-negative");
            }
            String memberId = body.get("memberId");
            Run run = nodeService.reportRun(id, runId, result, artifacts, costTokens, costUsd, memberId);
            return ResponseEntity.ok(run);
        } catch (NumberFormatException e) {
            return ControllerResponses.validation(auditService, "Invalid numeric field: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<?> revoke(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Node node = nodeService.revoke(id, actor);
            return ResponseEntity.ok(node);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateNode(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Node node = nodeService.updateMetadata(
                id,
                body.get("name"),
                body.get("region"),
                actor
            );
            return ResponseEntity.ok(node);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        }
    }
}
