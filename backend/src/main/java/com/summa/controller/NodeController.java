package com.summa.controller;

import com.summa.exception.EntityNotFoundException;
import com.summa.constants.Defaults;
import com.summa.model.Node;
import com.summa.model.Run;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.security.WriteGate;
import com.summa.service.AuditService;
import com.summa.service.MemberService;
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
    private final MemberService memberService;

    public NodeController(NodeService nodeService, AuditService auditService, WriteGate writeGate, MemberService memberService) {
        this.nodeService = nodeService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<?> listNodes() {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        return ResponseEntity.ok(nodeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getNode(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        Optional<Node> entOpt = nodeService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Node not found: " + id);
    }

    @PostMapping("/enroll")
    public ResponseEntity<?> enroll(@RequestBody Map<String, String> body) {
        // S1: Require authenticated admin for node enrollment — prevent unauthenticated node creation.
        // NodeAuthFilter strips signature verification for this path, so we gate on JWT actor instead.
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        if (Defaults.SYSTEM_ACTOR.equals(actor)) {
            return ControllerResponses.gate(auditService, "Admin authentication required to enroll nodes");
        }
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, "Only admins can enroll nodes");
        }
        String name = body.get("name");
        String kind = body.get("kind");
        String pubkey = body.get("pubkey");
        if (name == null || name.isBlank() || kind == null || kind.isBlank() || pubkey == null || pubkey.isBlank()) {
            return ControllerResponses.validation(auditService, "name, kind, and pubkey are required");
        }
        if (!pubkey.matches(Defaults.PUBKEY_REGEX)) {
            return ControllerResponses.validation(auditService, "pubkey must be a valid base64-encoded public key");
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
            Node node = nodeService.heartbeat(id, actor, body.get("capabilities"));
            return ResponseEntity.ok(node);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        } catch (EntityNotFoundException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
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
            int currentEpoch;
            try {
                currentEpoch = epochStr != null ? Integer.parseInt(epochStr.trim()) : 0;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid epoch value: " + epochStr);
            }
            if (currentEpoch < 0) {
                throw new IllegalArgumentException("epoch must be non-negative");
            }
            Node node = nodeService.claimWorkspace(id, workspaceId, currentEpoch);
            return ResponseEntity.ok(node);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        } catch (EntityNotFoundException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
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
        } catch (EntityNotFoundException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
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
                try {
                    costTokens = Long.parseLong(costTokensStr.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid costTokens value: " + costTokensStr);
                }
                if (costTokens < 0) throw new IllegalArgumentException("costTokens must be non-negative");
            }
            String costUsdStr = body.get("costUsd");
            double costUsd = 0.0;
            if (costUsdStr != null && !costUsdStr.isBlank()) {
                try {
                    costUsd = Double.parseDouble(costUsdStr.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid costUsd value: " + costUsdStr);
                }
                if (costUsd < 0 || Double.isInfinite(costUsd) || Double.isNaN(costUsd)) throw new IllegalArgumentException("costUsd must be non-negative and finite");
            }
            String memberId = body.get("memberId");
            Run run = nodeService.reportRun(id, runId, result, artifacts, costTokens, costUsd, memberId);
            return ResponseEntity.ok(run);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        } catch (EntityNotFoundException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
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
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        } catch (EntityNotFoundException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
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
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        } catch (EntityNotFoundException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }
}
