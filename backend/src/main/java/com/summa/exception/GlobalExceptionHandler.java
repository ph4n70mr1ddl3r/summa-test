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

    private ResponseEntity<Map<String, Object>> auditAndRespond(String auditAction, String errorCode,
            String message, HttpStatus status) {
        String actor = currentActor();
        AuditEvent audit = auditService.log(actor, auditAction, "http_request", errorCode, message);
        if (audit == null) {
            return ResponseEntity.status(status).body(Map.of("code", errorCode, "message", message));
        }
        return ResponseEntity.status(status).body(Map.of(
            "code", errorCode,
            "message", message,
            "audit_event_id", audit.getId()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return auditAndRespond("REFUSAL", "validation", e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException e) {
        return auditAndRespond("REFUSAL", "conflict", e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        return auditAndRespond("REFUSAL", "gate", e.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException e) {
        return auditAndRespond("REFUSAL", "not_found", e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        // Never leak SQL/schema text to clients — log the detail, return a generic code.
        log.warn("Data integrity conflict: {}", e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage() : e.getMessage());
        String message = "Resource conflict: the request violates a uniqueness or integrity constraint";
        return auditAndRespond("REFUSAL", "resource_conflict", message, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        log.error("Unhandled exception", e);
        return auditAndRespond("REFUSAL", "internal_error", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
