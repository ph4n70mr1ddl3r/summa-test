package com.summa.controller;

import com.summa.service.MemoryService;
import com.summa.model.MemoryItem;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import java.util.List;
import java.util.Map;

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
        return memoryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createMemory(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            MemoryItem item = memoryService.create(
                body.get("tier"),
                actor,
                body.get("workspaceId"),
                body.get("contentMd"),
                body.get("provenance"),
                Boolean.parseBoolean(body.getOrDefault("tainted", "false"))
            );
            return ResponseEntity.ok(item);
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

    @PostMapping("/{id}/review")
    public ResponseEntity<?> review(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActor() != null ? RbacAuthorizationFilter.getCurrentActor() : "system";
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            MemoryItem item = memoryService.review(id, actor);
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }
}
