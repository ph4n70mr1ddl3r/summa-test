package com.summa.service;

import com.summa.repository.AuditEventRepository;
import com.summa.model.AuditEvent;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuditService {
    // Patterns to redact sensitive data from audit log details
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)(password|passwd|pwd|secret|token_hash)\\s*[:=]\\s*\"[^\"]{3,}\"");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)(email|mail)\\s*[:=]\\s*\"[^\"]+@[^\"]+\"");

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository auditEventRepository, ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    public AuditEvent log(String actor, String action, String objectType, String objectId, String detail) {
        AuditEvent event = new AuditEvent();
        event.setId(UUID.randomUUID().toString());
        event.setActor(actor);
        event.setAction(action);
        event.setObjectType(objectType);
        event.setObjectId(objectId);
        event.setDetail(detail != null && !detail.isBlank() ? sanitizeJson(sanitizeSensitive(detail)) : "{}");
        event.setOrigin("live");
        return auditEventRepository.save(event);
    }

    public AuditEvent logSystem(String action, String objectType, String objectId, String detail) {
        return log("system", action, objectType, objectId, detail);
    }

    public AuditEvent logWithNode(String actor, String action, String objectType, String objectId,
                                     String nodeId, String detail) {
        AuditEvent event = new AuditEvent();
        event.setId(UUID.randomUUID().toString());
        event.setActor(actor);
        event.setAction(action);
        event.setObjectType(objectType);
        event.setObjectId(objectId);
        event.setDetail(detail != null && !detail.isBlank() ? sanitizeJson(sanitizeSensitive(detail)) : "{}");
        event.setOrigin("live");
        event.setNodeId(nodeId);
        return auditEventRepository.save(event);
    }

    private String sanitizeJson(String detail) {
        if (detail == null || detail.isBlank()) {
            return "{}";
        }
        try {
            objectMapper.readTree(detail);
            return detail;
        } catch (Exception e) {
            return "{}";
        }
    }

    private String sanitizeSensitive(String detail) {
        if (detail == null) return "{}";
        String sanitized = PASSWORD_PATTERN.matcher(detail).replaceAll("\"$1\":\"[REDACTED]\"");
        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("\"$1\":\"[REDACTED]\"");
        return sanitized;
    }
}
