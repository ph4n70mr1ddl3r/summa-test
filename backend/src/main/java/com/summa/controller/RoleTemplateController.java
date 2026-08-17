package com.summa.controller;

import com.summa.service.RoleTemplateService;
import com.summa.model.RoleTemplate;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/role-templates")
public class RoleTemplateController {
    private final RoleTemplateService templateService;
    private final AuditService auditService;

    public RoleTemplateController(RoleTemplateService templateService, AuditService auditService) {
        this.templateService = templateService;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<RoleTemplate>> listTemplates() {
        return ResponseEntity.ok(templateService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTemplate(@PathVariable String id) {
        return templateService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createTemplate(@RequestBody Map<String, String> body,
                                             @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            RoleTemplate template = templateService.create(
                body.get("name"),
                body.get("class"),
                body.get("body"),
                body.get("defaultScopes")
            );
            return ResponseEntity.ok(template);
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

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publish(@PathVariable String id,
                                      @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            RoleTemplate template = templateService.publish(id, actor);
            return ResponseEntity.ok(template);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<?> retire(@PathVariable String id,
                                     @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            RoleTemplate template = templateService.retire(id, actor);
            return ResponseEntity.ok(template);
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }
}
