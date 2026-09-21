package com.summa.controller;

import com.summa.security.RbacAuthorizationFilter;
import com.summa.security.WriteGate;
import com.summa.service.AuditService;
import com.summa.service.SecretsScanner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/admin/secrets")
public class SecretsController {
    private static final int MAX_SCAN_CONTENT_LENGTH = 1_000_000;

    private final SecretsScanner scanner;
    private final AuditService auditService;
    private final WriteGate writeGate;

    public SecretsController(SecretsScanner scanner, AuditService auditService, WriteGate writeGate) {
        this.scanner = scanner;
        this.auditService = auditService;
        this.writeGate = writeGate;
    }

    @PostMapping("/scan")
    public ResponseEntity<?> scan(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        String content = body.get("content");
        if (content == null) {
            return ControllerResponses.validation(auditService, "content required");
        }
        if (content.length() > MAX_SCAN_CONTENT_LENGTH) {
            return ControllerResponses.validation(auditService, "content exceeds maximum scan length");
        }
        var findings = scanner.scan(content);
        return ResponseEntity.ok(Map.of(
            "hasSecrets", !findings.isEmpty(),
            "count", findings.size()
        ));
    }
}
