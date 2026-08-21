package com.summa.exception;

import com.summa.model.AuditEvent;
import com.summa.service.AuditService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final AuditService auditService;

    public GlobalExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        AuditEvent audit = auditService.logSystem("REFUSAL", "http_request", e.getMessage(), null);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of(
                    "code", "validation",
                    "message", e.getMessage(),
                    "audit_event_id", audit.getId()
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        AuditEvent audit = auditService.logSystem("REFUSAL", "http_request", e.getMessage(), null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "code", "gate",
                    "message", e.getMessage(),
                    "audit_event_id", audit.getId()
                ));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException e) {
        AuditEvent audit = auditService.logSystem("REFUSAL", "http_request", e.getMessage(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "code", "not_found",
                    "message", e.getMessage(),
                    "audit_event_id", audit.getId()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        AuditEvent audit = auditService.logSystem("REFUSAL", "conflict", e.getMessage(), null);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                    "code", "conflict",
                    "message", "Resource conflict: " + e.getMostSpecificCause().getMessage(),
                    "audit_event_id", audit.getId()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        AuditEvent audit = auditService.logSystem("REFUSAL", "http_request", e.getMessage(), null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "code", "internal",
                    "message", "Internal server error",
                    "audit_event_id", audit.getId()
                ));
    }
}
