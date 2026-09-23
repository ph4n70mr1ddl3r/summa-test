package com.summa.service;

import com.summa.repository.AuditEventRepository;
import com.summa.model.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Pattern;
import static com.summa.constants.Defaults.SYSTEM_ACTOR;
import com.summa.util.JsonHelpers;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    // Patterns to redact sensitive data from audit log details
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "(?i)(password|passwd|pwd|secret|token_hash)\\s*[:=]\\s*\"[^\"]{3,}\"|" +
        "(?i)(password|passwd|pwd|secret|token_hash)\\s*[:=]\\s*([^\\s,;}{\"]{3,})"
    );
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)(email|mail)\\s*[:=]\\s*[\"']([^\"']+@[^\"]+)[\"']");

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
        event.setActor(actor != null ? actor : SYSTEM_ACTOR);
        event.setAction(action != null ? action : "unknown");
        event.setObjectType(objectType != null ? objectType : "unknown");
        event.setObjectId(objectId != null ? objectId : "unknown");
        event.setDetail(detail != null && !detail.isBlank() ? sanitizeJson(sanitizeSensitive(detail)) : "{}");
        event.setOrigin("live");
        event.setNodeId(nodeId);
        return auditEventRepository.save(event);
    }

    public AuditEvent logSystem(String action, String objectType, String objectId, String detail) {
        return log(SYSTEM_ACTOR, action, objectType, objectId, null, detail);
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
            return "{\"raw\":" + JsonHelpers.jsonString(detail) + "}";
        }
    }

    private String sanitizeSensitive(String detail) {
        if (detail == null) return "{}";
        // Sanitize by rebuilding as a proper JSON object when possible,
        // otherwise apply regex redaction to the raw string.
        try {
            // Try to parse as JSON object, redact sensitive keys, then re-serialize
            JsonNode node = objectMapper.readTree(detail);
            if (node.isObject()) {
                ObjectNode obj = (ObjectNode) node;
                Iterator<String> fields = obj.fieldNames();
                while (fields.hasNext()) {
                    String key = fields.next();
                    if (key.toLowerCase().matches(".*(password|passwd|pwd|secret|token_hash|api_key|apikey|access_key|auth_token|bearer_token|session_token).*")) {
                        obj.set(key, objectMapper.valueToTree("[REDACTED]"));
                    }
                }
                return obj.toString();
            }
        } catch (Exception e) {
            // Not valid JSON — fall through to regex-based redaction
            log.trace("Detail is not valid JSON, using regex redaction: {}", e.getMessage());
        }
        String sanitized = PASSWORD_PATTERN.matcher(detail).replaceAll("\"$1\":\"[REDACTED]\"");
        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("\"$1\":\"[REDACTED]\"");
        return sanitized;
    }
}
