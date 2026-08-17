package com.summa.controller;

import com.summa.service.SecretsScanner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/admin/secrets")
public class SecretsController {
    private final SecretsScanner scanner;

    public SecretsController(SecretsScanner scanner) {
        this.scanner = scanner;
    }

    @PostMapping("/scan")
    public ResponseEntity<?> scan(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "content required"));
        }
        var findings = scanner.scan(content);
        return ResponseEntity.ok(Map.of(
            "hasSecrets", !findings.isEmpty(),
            "patterns", findings,
            "count", findings.size()
        ));
    }
}
