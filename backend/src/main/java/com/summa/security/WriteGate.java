package com.summa.security;

import com.summa.model.AuditEvent;
import com.summa.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class WriteGate {

    private final AuditService auditService;

    public WriteGate(AuditService auditService) {
        this.auditService = auditService;
    }

    public ResponseEntity<Map<String, Object>> enforce(String actor) {
        if (!RbacAuthorizationFilter.isWriteAllowed()) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "write_gate",
                    "Viewer does not have write permission", actor);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "eligibility", "message", "Viewer does not have write permission",
                            "audit_event_id", audit.getId()));
        }
        return null;
    }
}
