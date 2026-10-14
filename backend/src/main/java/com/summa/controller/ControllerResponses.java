package com.summa.controller;

import com.summa.model.AuditEvent;
import com.summa.service.AuditService;
import com.summa.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.Optional;

/**
 * Shared response helpers for all REST controllers.
 * Centralizes the REFUSAL audit pattern so every write endpoint
 * logs with the same structure and status codes.
 */
public final class ControllerResponses {
    private ControllerResponses() {}

    public static ResponseEntity<?> requireAuth(AuditService audit, MemberService memberService, String actor) {
        if ("system".equals(actor)) return null;
        Optional<com.summa.model.Human> humanOpt = memberService.findHuman(actor);
        Optional<com.summa.model.Agent> agentOpt = memberService.findAgent(actor);
        boolean hasAccess = humanOpt.isPresent() || agentOpt.isPresent();
        return hasAccess ? null : gate(audit, actor, "Authentication required");
    }

    public static ResponseEntity<Map<String, Object>> validation(AuditService audit, String message) {
        AuditEvent event = audit.logSystem("REFUSAL", "validation", null, message);
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", "validation");
        body.put("message", message);
        body.put("audit_event_id", event.getId());
        body.put("ask_id", null);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    public static ResponseEntity<Map<String, Object>> validation(AuditEvent existingAudit, String message) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", "validation");
        body.put("message", message);
        body.put("audit_event_id", existingAudit.getId());
        body.put("ask_id", null);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    public static ResponseEntity<Map<String, Object>> validation(AuditService audit, String actor, String message) {
        AuditEvent event = audit.log(actor, "REFUSAL", "validation", null, message);
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", "validation");
        body.put("message", message);
        body.put("audit_event_id", event.getId());
        body.put("ask_id", null);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    public static ResponseEntity<Map<String, Object>> gate(AuditService audit, String message) {
        AuditEvent event = audit.logSystem("REFUSAL", "gate", null, message);
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", "gate");
        body.put("message", message);
        body.put("audit_event_id", event.getId());
        body.put("ask_id", null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    public static ResponseEntity<Map<String, Object>> gate(AuditEvent existingAudit, String message) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", "gate");
        body.put("message", message);
        body.put("audit_event_id", existingAudit.getId());
        body.put("ask_id", null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    public static ResponseEntity<Map<String, Object>> gate(AuditService audit, String actor, String message) {
        AuditEvent event = audit.log(actor, "REFUSAL", "gate", null, message);
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", "gate");
        body.put("message", message);
        body.put("audit_event_id", event.getId());
        body.put("ask_id", null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    public static ResponseEntity<Map<String, Object>> notFound(AuditService audit, String message) {
        AuditEvent event = audit.logSystem("NOT_FOUND", "not_found", null, message);
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", "not_found");
        body.put("message", message);
        body.put("audit_event_id", event.getId());
        body.put("ask_id", null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    public static ResponseEntity<Map<String, Object>> notFound(AuditEvent existingAudit, String message) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", "not_found");
        body.put("message", message);
        body.put("audit_event_id", existingAudit.getId());
        body.put("ask_id", null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    public static ResponseEntity<Map<String, Object>> notFound(AuditService audit, String actor, String message) {
        AuditEvent event = audit.log(actor, "NOT_FOUND", "not_found", null, message);
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", "not_found");
        body.put("message", message);
        body.put("audit_event_id", event.getId());
        body.put("ask_id", null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    public static ResponseEntity<Map<String, Object>> conflict(AuditService audit, String message) {
        AuditEvent event = audit.logSystem("REFUSAL", "conflict", null, message);
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", "conflict");
        body.put("message", message);
        body.put("audit_event_id", event.getId());
        body.put("ask_id", null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    public static ResponseEntity<Map<String, Object>> conflict(AuditEvent existingAudit, String message) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", "conflict");
        body.put("message", message);
        body.put("audit_event_id", existingAudit.getId());
        body.put("ask_id", null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    public static ResponseEntity<Map<String, Object>> conflict(AuditService audit, String actor, String message) {
        AuditEvent event = audit.log(actor, "REFUSAL", "conflict", null, message);
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("code", "conflict");
        body.put("message", message);
        body.put("audit_event_id", event.getId());
        body.put("ask_id", null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    public static ResponseEntity<Map<String, Object>> internalError(AuditService audit, String message) {
        AuditEvent event = audit.logSystem("ERROR", "internal", null, message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", "internal", "message", message, "audit_event_id", event.getId()));
    }

    public static ResponseEntity<Map<String, Object>> internalError(AuditService audit, String actor, String message) {
        AuditEvent event = audit.log(actor, "ERROR", "internal", null, message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", "internal", "message", message, "audit_event_id", event.getId()));
    }

    public static ResponseEntity<Map<String, Object>> serviceUnavailable(AuditService audit, String message) {
        AuditEvent event = audit.logSystem("REFUSAL", "service_unavailable", null, message);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code", "service_unavailable", "message", message, "audit_event_id", event.getId()));
    }

    public static ResponseEntity<Map<String, Object>> tooManyRequests(AuditService audit, String message, long remainingAttempts) {
        AuditEvent event = audit.logSystem("REFUSAL", "rate_limited", null, message);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("code", "rate_limited", "message", message, "audit_event_id", event.getId(), "remainingAttempts", remainingAttempts));
    }

    public static ResponseEntity<Map<String, Object>> tooManyRequests(AuditEvent existingAudit, String message, long remainingAttempts) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("code", "rate_limited", "message", message, "audit_event_id", existingAudit.getId(), "remainingAttempts", remainingAttempts));
    }

    public static ResponseEntity<Map<String, Object>> tooManyRequests(AuditService audit, String actor, String message, long remainingAttempts) {
        AuditEvent event = audit.log(actor, "REFUSAL", "rate_limited", null, message);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("code", "rate_limited", "message", message, "audit_event_id", event.getId(), "remainingAttempts", remainingAttempts));
    }
}
