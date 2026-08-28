package com.summa.controller;

import com.summa.service.NodeService;
import com.summa.model.Node;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
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
        try {
            Node node = nodeService.enroll(
                body.get("name"),
                body.get("kind"),
                body.get("pubkey")
            );
            return ResponseEntity.ok(Map.of(
                "id", node.getId(),
                "enrollmentToken", nodeService.generateEnrollmentToken(node.getId())
            ));
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

    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<?> heartbeat(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            Node node = nodeService.heartbeat(id, body.get("capabilities"));
            return ResponseEntity.ok(node);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/claims")
    public ResponseEntity<?> claimWorkspace(@PathVariable String id, @RequestBody Map<String, String> body) {
        // API-060: acquire or renew a workspace claim as epoch-fenced lease
        // TODO: implement WorkspaceService integration for epoch-fenced leases
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("code", "not_implemented", "message", "Workspace claim leasing is not yet implemented"));
    }

    @PostMapping("/{id}/work/pull")
    public ResponseEntity<?> pullWork(@PathVariable String id) {
        // API-060: fetch queued runs for workspaces the node holds a live claim on
        // TODO: implement work queue integration with WorkspaceService
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("code", "not_implemented", "message", "Work pull is not yet implemented"));
    }

    @PostMapping("/{id}/runs/{runId}/report")
    public ResponseEntity<?> reportRun(@PathVariable String id, @PathVariable String runId,
                                        @RequestBody Map<String, Object> body) {
        // API-060: land results, artifacts, and spend ledger lines
        // TODO: implement run result landing with spend ledger integration
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("code", "not_implemented", "message", "Run reporting is not yet implemented"));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<?> revoke(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Node node = nodeService.revoke(id, actor);
            return ResponseEntity.ok(node);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateNode(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
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
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }
}
