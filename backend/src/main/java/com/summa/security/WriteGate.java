package com.summa.security;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class WriteGate {

    public ResponseEntity<Map<String, Object>> enforce(String actor) {
        if (!RbacAuthorizationFilter.isWriteAllowed()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "eligibility", "message", "Viewer does not have write permission"));
        }
        return null;
    }
}
