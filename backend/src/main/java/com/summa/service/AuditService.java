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
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "(?i)(password|passwd|pwd|secret|token_hash)\\s*[:=]\\s*\"[^\"]{3,}\"|" +
        "(?i)(password|passwd|pwd|secret|token_hash)\\s*[:=]\\s*([^\\s,;}{\"]{3,})"
    );
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)(email|mail)\\s*[:=]\\s*\"[^\"]+@[^\"]+\"");

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository auditEventRepository, ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    public AuditEvent log(String actor, String action, String objectType, String objectId, String detail) {
        return log(actor, action, objectType, objectId, null, detail);
    }

    public AuditEvent log(String actor, String action, String objectType, String objectId, String nodeId, String detail) {
        AuditEvent event = new AuditEvent();
        event.setId(UUID.randomUUID().toString());
        event.setActor(actor != null ? actor : "system");
        event.setAction(action);
        event.setObjectType(objectType);
        event.setObjectId(objectId);
        event.setDetail(detail != null && !detail.isBlank() ? sanitizeJson(sanitizeSensitive(detail)) : "{}");
        event.setOrigin("live");
        event.setNodeId(nodeId);
        return auditEventRepository.save(event);
    }

    public AuditEvent logSystem(String action, String objectType, String objectId, String detail) {
        return log("system", action, objectType, objectId, null, detail);
    }

    public AuditEvent logWithNode(String actor, String action, String objectType, String objectId,
                                      String nodeId, String detail) {
        return log(actor, action, objectType, objectId, nodeId, detail);
    }

    private String sanitizeJson(String detail) {
        if (detail == null || detail.isBlank()) {
            return "{}";
        }
        try {
            objectMapper.readTree(detail);
            return detail;
        } catch (Exception e) {
            return "{\"raw\":" + jsonStringForAudit(detail) + "}";
        }
    }

    private String jsonStringForAudit(String value) {
        if (value == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append("\"").toString();
    }

    private String sanitizeSensitive(String detail) {
        if (detail == null) return "{}";
        String sanitized = PASSWORD_PATTERN.matcher(detail).replaceAll("\"$1\":\"[REDACTED]\"");
        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("\"$1\":\"[REDACTED]\"");
        return sanitized;
    }
}
