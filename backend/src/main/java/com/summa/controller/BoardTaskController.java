package com.summa.controller;

import com.summa.service.BoardTaskService;
import com.summa.model.BoardTask;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/board-tasks")
public class BoardTaskController {
    private final BoardTaskService taskService;
    private final AuditService auditService;

    public BoardTaskController(BoardTaskService taskService, AuditService auditService) {
        this.taskService = taskService;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<BoardTask>> listTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) String initiativeId) {
        if (assigneeId != null) {
            return ResponseEntity.ok(taskService.findByAssignee(assigneeId));
        }
        if (initiativeId != null) {
            return ResponseEntity.ok(taskService.findByInitiative(initiativeId));
        }
        if (status != null) {
            return ResponseEntity.ok(taskService.findAll().stream()
                .filter(t -> status.equals(t.getStatus()))
                .toList());
        }
        return ResponseEntity.ok(taskService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTask(@PathVariable String id) {
        return taskService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Map<String, String> body,
                                         @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Integer priority = body.containsKey("priority") ? Integer.parseInt(body.get("priority")) : null;
            Instant dueAt = body.containsKey("dueAt") ? Instant.parse(body.get("dueAt")) : null;
            
            BoardTask task = taskService.create(
                body.get("title"),
                body.get("description"),
                actor,
                body.get("assigneeMemberId"),
                body.get("initiativeId"),
                priority,
                dueAt
            );
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.badRequest().body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<?> assign(@PathVariable String id, @RequestBody Map<String, String> body,
                                     @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            BoardTask task = taskService.assign(id, body.get("assigneeMemberId"), actor);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable String id,
                                       @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            BoardTask task = taskService.complete(id, actor);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/unassign")
    public ResponseEntity<?> unassign(@PathVariable String id,
                                       @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            BoardTask task = taskService.unassign(id, actor);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
