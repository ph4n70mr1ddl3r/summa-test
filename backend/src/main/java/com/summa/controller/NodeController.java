package com.summa.controller;

import com.summa.model.AuditEvent;
import com.summa.model.Node;
import com.summa.model.Run;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.security.WriteGate;
import com.summa.service.AuditService;
import com.summa.service.NodeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

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
        return nodeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/enroll")
    public ResponseEntity<?> enroll(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String kind = body.get("kind");
        String pubkey = body.get("pubkey");
        if (name == null || name.isBlank() || kind == null || kind.isBlank() || pubkey == null || pubkey.isBlank()) {
            return ControllerResponses.validation(auditService, "name, kind, and pubkey are required");
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
        try {
            Node node = nodeService.heartbeat(id, body.get("capabilities"));
            return ResponseEntity.ok(node);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/claims")
    public ResponseEntity<?> claimWorkspace(@PathVariable String id, @RequestBody Map<String, Object> body) {
        // API-060: acquire or renew a workspace claim as epoch-fenced lease
        try {
            String workspaceId = (String) body.get("workspaceId");
            if (workspaceId == null || workspaceId.isBlank()) {
                throw new IllegalArgumentException("workspaceId is required");
            }
            int currentEpoch = body.get("epoch") != null ? Integer.parseInt(body.get("epoch").toString()) : 0;
            Node node = nodeService.claimWorkspace(id, workspaceId, currentEpoch);
            return ResponseEntity.ok(node);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "claim", e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        } catch (IllegalStateException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "claim", e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/work/pull")
    public ResponseEntity<?> pullWork(@PathVariable String id) {
        // API-060: fetch queued runs for workspaces the node holds a live claim on
        try {
            List<Run> runs = nodeService.pullWork(id);
            return ResponseEntity.ok(runs);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "pull_work", e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        } catch (IllegalStateException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "pull_work", e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/runs/{runId}/report")
    public ResponseEntity<?> reportRun(@PathVariable String id, @PathVariable String runId,
                                        @RequestBody Map<String, Object> body) {
        // API-060: land results, artifacts, and spend ledger lines
        try {
            String result = (String) body.get("result");
            @SuppressWarnings("unchecked")
            String artifacts = body.get("artifacts") != null ? body.get("artifacts").toString() : null;
            long costTokens = body.get("costTokens") != null ? ((Number) body.get("costTokens")).longValue() : 0L;
            double costUsd = body.get("costUsd") != null ? ((Number) body.get("costUsd")).doubleValue() : 0.0;
            String memberId = (String) body.get("memberId");
            Run run = nodeService.reportRun(id, runId, result, artifacts, costTokens, costUsd, memberId);
            return ResponseEntity.ok(run);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "report_run", e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        } catch (IllegalStateException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "report_run", e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
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
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }
}
