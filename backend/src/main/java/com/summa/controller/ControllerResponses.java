package com.summa.controller;

import com.summa.model.AuditEvent;
import com.summa.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;

/**
 * Shared response helpers for all REST controllers.
 * Centralizes the REFUSAL audit pattern so every write endpoint
 * logs with the same structure and status codes.
 */
public final class ControllerResponses {
    private ControllerResponses() {}

    public static ResponseEntity<Map<String, Object>> validation(AuditService audit, String message) {
        AuditEvent event = audit.logSystem("REFUSAL", "validation", null, message);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("code", "validation", "message", message, "audit_event_id", event.getId()));
    }

    public static ResponseEntity<Map<String, Object>> validation(AuditEvent existingAudit, String message) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("code", "validation", "message", message, "audit_event_id", existingAudit.getId()));
    }

    public static ResponseEntity<Map<String, Object>> validation(AuditService audit, String actor, String message) {
        AuditEvent event = audit.log(actor, "REFUSAL", "validation", null, message);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("code", "validation", "message", message, "audit_event_id", event.getId()));
    }

    public static ResponseEntity<Map<String, Object>> gate(AuditService audit, String message) {
        AuditEvent event = audit.logSystem("REFUSAL", "gate", null, message);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", "gate", "message", message, "audit_event_id", event.getId()));
    }

    public static ResponseEntity<Map<String, Object>> gate(AuditEvent existingAudit, String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", "gate", "message", message, "audit_event_id", existingAudit.getId()));
    }

    public static ResponseEntity<Map<String, Object>> gate(AuditService audit, String actor, String message) {
        AuditEvent event = audit.log(actor, "REFUSAL", "gate", null, message);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", "gate", "message", message, "audit_event_id", event.getId()));
    }

    public static ResponseEntity<Map<String, Object>> notFound(AuditService audit, String message) {
        AuditEvent event = audit.logSystem("REFUSAL", "not_found", null, message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "not_found", "message", message, "audit_event_id", event.getId()));
    }

    public static ResponseEntity<Map<String, Object>> notFound(AuditEvent existingAudit, String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "not_found", "message", message, "audit_event_id", existingAudit.getId()));
    }

    public static ResponseEntity<Map<String, Object>> notFound(AuditService audit, String actor, String message) {
        AuditEvent event = audit.log(actor, "REFUSAL", "not_found", null, message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "not_found", "message", message, "audit_event_id", event.getId()));
    }

    public static ResponseEntity<Map<String, Object>> conflict(AuditService audit, String message) {
        AuditEvent event = audit.logSystem("REFUSAL", "conflict", null, message);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", "conflict", "message", message, "audit_event_id", event.getId()));
    }

    public static ResponseEntity<Map<String, Object>> conflict(AuditEvent existingAudit, String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", "conflict", "message", message, "audit_event_id", existingAudit.getId()));
    }

    public static ResponseEntity<Map<String, Object>> conflict(AuditService audit, String actor, String message) {
        AuditEvent event = audit.log(actor, "REFUSAL", "conflict", null, message);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", "conflict", "message", message, "audit_event_id", event.getId()));
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
