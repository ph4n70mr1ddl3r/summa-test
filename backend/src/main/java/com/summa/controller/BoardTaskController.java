package com.summa.controller;

import com.summa.service.BoardTaskService;
import com.summa.model.BoardTask;
import com.summa.service.AuditService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.util.JsonHelpers;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/board-tasks")
public class BoardTaskController {
    private final BoardTaskService taskService;
    private final AuditService auditService;
    private final WriteGate writeGate;

    public BoardTaskController(BoardTaskService taskService, AuditService auditService, WriteGate writeGate) {
        this.taskService = taskService;
        this.auditService = auditService;
        this.writeGate = writeGate;
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
            return ResponseEntity.ok(taskService.findByStatus(status));
        }
        return ResponseEntity.ok(taskService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTask(@PathVariable String id) {
        Optional<BoardTask> entOpt = taskService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Task not found: " + id);
    }

    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Integer priority = null;
            if (body.containsKey("priority")) {
                try {
                    priority = Integer.parseInt(body.get("priority"));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("priority must be a valid integer");
                }
            }
            Instant dueAt = JsonHelpers.parseOptionalInstant(body.get("dueAt"), "dueAt");
            
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
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<?> assign(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            BoardTask task = taskService.assign(id, body.get("assigneeMemberId"), actor);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            BoardTask task = taskService.complete(id, actor);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/unassign")
    public ResponseEntity<?> unassign(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            BoardTask task = taskService.unassign(id, actor);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }
}
