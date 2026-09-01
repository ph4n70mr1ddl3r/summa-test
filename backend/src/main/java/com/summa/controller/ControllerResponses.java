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
        AuditEvent event = audit.logSystem("REFUSAL", "validation", message, null);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("code", "validation", "message", message, "audit_event_id", event.getId()));
    }

    public static ResponseEntity<Map<String, Object>> gate(AuditService audit, String message) {
        AuditEvent event = audit.logSystem("REFUSAL", "gate", message, null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", "gate", "message", message, "audit_event_id", event.getId()));
    }

    public static ResponseEntity<Map<String, Object>> notFound(AuditService audit, String message) {
        AuditEvent event = audit.logSystem("REFUSAL", "not_found", message, null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "not_found", "message", message, "audit_event_id", event.getId()));
    }
}
