package com.summa.controller;

import com.summa.service.MemoryService;
import com.summa.model.MemoryItem;
import com.summa.service.AuditService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/memory")
public class MemoryController {
    private final MemoryService memoryService;
    private final AuditService auditService;
    private final WriteGate writeGate;

    public MemoryController(MemoryService memoryService, AuditService auditService, WriteGate writeGate) {
        this.memoryService = memoryService;
        this.auditService = auditService;
        this.writeGate = writeGate;
    }

    @GetMapping
    public ResponseEntity<List<MemoryItem>> listMemory(
            @RequestParam(required = false) String memberId,
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) Boolean tainted) {
        if (memberId != null) {
            return ResponseEntity.ok(memoryService.findByMember(memberId));
        }
        if (workspaceId != null) {
            return ResponseEntity.ok(memoryService.findByWorkspace(workspaceId));
        }
        if (Boolean.TRUE.equals(tainted)) {
            return ResponseEntity.ok(memoryService.findTainted());
        }
        return ResponseEntity.ok(memoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMemory(@PathVariable String id) {
        Optional<MemoryItem> entOpt = memoryService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Memory item not found: " + id);
    }

    @PostMapping
    public ResponseEntity<?> createMemory(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            String tier = body.get("tier");
            if (tier == null || tier.isBlank()) {
                throw new IllegalArgumentException("tier is required");
            }
            MemoryItem item = memoryService.create(
                tier,
                actor,
                body.get("workspaceId"),
                body.get("contentMd"),
                body.get("provenance"),
                Boolean.parseBoolean(body.getOrDefault("tainted", "false"))
            );
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<?> review(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            MemoryItem item = memoryService.review(id, actor);
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }
}
