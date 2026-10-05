package com.summa.exception;

import com.summa.model.AuditEvent;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditService auditService;

    public GlobalExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    private String currentActor() {
        try {
            return RbacAuthorizationFilter.getCurrentActorOrDefault();
        } catch (Exception e) {
            return null;
        }
    }

    private ResponseEntity<Map<String, Object>> auditAndRespond(String auditAction, String objectType,
            String objectId, String message, HttpStatus status) {
        String actor = currentActor();
        // Fall back to system actor so audit entries are never created with a null actor
        if (actor == null) actor = com.summa.constants.Defaults.SYSTEM_ACTOR;
        try {
            auditService.log(actor, auditAction, objectType, objectId, message);
        } catch (Exception e) {
            log.warn("Audit log failed, continuing without audit entry: {}", e.getMessage());
        }
        return ResponseEntity.status(status).body(Map.of(
            "code", objectType, "message", message, "audit_event_id", "audit_failed"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return auditAndRespond("REFUSAL", "validation", null, e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException e) {
        return auditAndRespond("REFUSAL", "conflict", null, e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        // Distinguish authorization refusals (e.g. WriteGate denial) from genuine server errors.
        // Use the exception message class hierarchy rather than substring matching for reliability.
        String msg = e.getMessage();
        if (msg != null && isAuthRefusal(msg)) {
            return auditAndRespond("REFUSAL", "gate", null, msg, HttpStatus.FORBIDDEN);
        }
        throw e;
    }

    private boolean isAuthRefusal(String msg) {
        // Match authorization refusal patterns produced by WriteGate, controllers, and services.
        // These correspond to messages thrown as IllegalStateException for permission denials.
        if (msg == null) return false;
        return msg.contains("does not have write permission")
                || msg.contains("Admin access required")
                || msg.contains("Authentication required")
                || msg.contains("admin required")
                || msg.contains("requires admin role")
                || msg.contains("requires admin")
                || msg.contains("Only the agent's owner or an admin")
                || msg.startsWith("gate refusal")
                || msg.contains("gate refusal");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException e) {
        return auditAndRespond("NOT_FOUND", "not_found", null, e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        // Never leak SQL/schema text to clients — log the detail, return a generic code.
        log.warn("Data integrity conflict: {}", e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage() : e.getMessage());
        String message = "Resource conflict: the request violates a uniqueness or integrity constraint";
        return auditAndRespond("REFUSAL", "resource_conflict", null, message, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        log.error("Unhandled exception", e);
        return auditAndRespond("ERROR", "internal_error", null, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
