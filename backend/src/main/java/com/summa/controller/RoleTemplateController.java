package com.summa.controller;

import com.summa.service.RoleTemplateService;
import com.summa.model.RoleTemplate;
import com.summa.service.AuditService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.service.MemberService;
import com.summa.enums.RoleTemplateClass;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/role-templates")
public class RoleTemplateController {
    private final RoleTemplateService templateService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final MemberService memberService;

    public RoleTemplateController(RoleTemplateService templateService, AuditService auditService, WriteGate writeGate, MemberService memberService) {
        this.templateService = templateService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<?> listTemplates() {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        return ResponseEntity.ok(templateService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTemplate(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
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
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Admin access required to create role templates");
        }
        try {
            if (body.get("name") == null || body.get("name").isBlank()) {
                throw new IllegalArgumentException("name is required");
            }
            if (body.get("class") == null || body.get("class").isBlank()) {
                throw new IllegalArgumentException("class is required");
            }
            // Validate the class against the catalog column's vocabulary
            // (DAT-010's role_templates.class — 'persistent' | 'ephemeral-subagent').
            // The spawn classes are three; the template classes are two, so
            // validating against the agent-class enum would accept a value the
            // column's CHECK rejects — a 409 integrity abort where the door owes
            // a 422 validation refusal (API-061).
            RoleTemplateClass.requireFromValue(body.get("class"));
            RoleTemplate template = templateService.create(
                body.get("name"),
                body.get("class"),
                body.get("body"),
                body.get("defaultScopes"),
                actor
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
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Admin access required to publish role templates");
        }
        try {
            RoleTemplate template = templateService.publish(id, actor);
            return ResponseEntity.ok(template);
        } catch (com.summa.exception.EntityNotFoundException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<?> retire(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Admin access required to retire role templates");
        }
        try {
            RoleTemplate template = templateService.retire(id, actor);
            return ResponseEntity.ok(template);
        } catch (com.summa.exception.EntityNotFoundException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }
}
