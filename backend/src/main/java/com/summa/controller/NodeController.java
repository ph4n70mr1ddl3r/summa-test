package com.summa.controller;

import com.summa.service.NodeService;
import com.summa.model.Node;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/nodes")
public class NodeController {
    private final NodeService nodeService;

    public NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
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
                "enrollmentToken", "temp-token-" + node.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<?> heartbeat(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            Node node = nodeService.heartbeat(id, body.get("capabilities"));
            return ResponseEntity.ok(node);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<?> revoke(@PathVariable String id,
                                     @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Node node = nodeService.revoke(id, actor);
            return ResponseEntity.ok(node);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateNode(@PathVariable String id, @RequestBody Map<String, String> body,
                                         @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Node node = nodeService.updateMetadata(
                id,
                body.get("name"),
                body.get("region"),
                actor
            );
            return ResponseEntity.ok(node);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
