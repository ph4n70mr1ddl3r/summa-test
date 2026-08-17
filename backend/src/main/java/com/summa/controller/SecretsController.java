package com.summa.controller;

import com.summa.service.SecretsScanner;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/admin/secrets")
public class SecretsController {
    private final SecretsScanner scanner;
    private final AuditService auditService;

    public SecretsController(SecretsScanner scanner, AuditService auditService) {
        this.scanner = scanner;
        this.auditService = auditService;
    }

    @PostMapping("/scan")
    public ResponseEntity<?> scan(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "validation", "content required", null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "validation", "message", "content required", "audit_event_id", audit.getId()));
        }
        var findings = scanner.scan(content);
        return ResponseEntity.ok(Map.of(
            "hasSecrets", !findings.isEmpty(),
            "patterns", findings,
            "count", findings.size()
        ));
    }
}
