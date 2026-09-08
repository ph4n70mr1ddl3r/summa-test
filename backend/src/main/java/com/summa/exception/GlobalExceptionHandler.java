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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        String actor = currentActor();
        AuditEvent audit = auditService.log(actor, "REFUSAL", "http_request", "validation", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of(
                    "code", "validation",
                    "message", e.getMessage(),
                    "audit_event_id", audit.getId()
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        String actor = currentActor();
        AuditEvent audit = auditService.log(actor, "REFUSAL", "http_request", "gate", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "code", "gate",
                    "message", e.getMessage(),
                    "audit_event_id", audit.getId()
                ));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException e) {
        String actor = currentActor();
        AuditEvent audit = auditService.log(actor, "REFUSAL", "http_request", "not_found", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "code", "not_found",
                    "message", e.getMessage(),
                    "audit_event_id", audit.getId()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        // Never leak SQL/schema text to clients — log the detail, return a generic code.
        log.warn("Data integrity conflict: {}", e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage() : e.getMessage());
        String actor = currentActor();
        AuditEvent audit = auditService.log(actor, "REFUSAL", "conflict", "resource_conflict", "Resource conflict: the request violates a uniqueness or integrity constraint");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                    "code", "conflict",
                    "message", "Resource conflict: the request violates a uniqueness or integrity constraint",
                    "audit_event_id", audit.getId()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        log.error("Unhandled exception", e);
        String actor = currentActor();
        AuditEvent audit = auditService.log(actor, "REFUSAL", "http_request", "internal_error", "Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "code", "internal",
                    "message", "Internal server error",
                    "audit_event_id", audit.getId()
                ));
    }
}
