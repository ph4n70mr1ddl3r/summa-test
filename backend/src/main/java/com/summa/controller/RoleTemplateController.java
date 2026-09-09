package com.summa.controller;

import com.summa.service.RoleTemplateService;
import com.summa.model.RoleTemplate;
import com.summa.service.AuditService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/role-templates")
public class RoleTemplateController {
    private final RoleTemplateService templateService;
    private final AuditService auditService;
    private final WriteGate writeGate;

    public RoleTemplateController(RoleTemplateService templateService, AuditService auditService, WriteGate writeGate) {
        this.templateService = templateService;
        this.auditService = auditService;
        this.writeGate = writeGate;
    }

    @GetMapping
    public ResponseEntity<List<RoleTemplate>> listTemplates() {
        return ResponseEntity.ok(templateService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTemplate(@PathVariable String id) {
        Optional<RoleTemplate> entOpt = templateService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Role template not found: " + id);
    }

    @PostMapping
    public ResponseEntity<?> createTemplate(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            RoleTemplate template = templateService.create(
                body.get("name"),
                body.get("class"),
                body.get("body"),
                body.get("defaultScopes")
            );
            return ResponseEntity.ok(template);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publish(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            RoleTemplate template = templateService.publish(id, actor);
            return ResponseEntity.ok(template);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<?> retire(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            RoleTemplate template = templateService.retire(id, actor);
            return ResponseEntity.ok(template);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }
}
