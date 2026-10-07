package com.summa.security;

import com.summa.controller.ControllerResponses;
import com.summa.model.AuditEvent;
import com.summa.service.AuditService;
import org.springframework.http.HttpStatus;
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
            return ControllerResponses.gate(auditService, actor, "Viewer does not have write permission");
        }
        return null;
    }
}
