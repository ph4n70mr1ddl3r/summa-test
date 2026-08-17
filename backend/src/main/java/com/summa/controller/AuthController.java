package com.summa.controller;

import com.summa.service.OrgService;
import com.summa.service.AuditService;
import com.summa.security.JwtUtil;
import com.summa.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final OrgService orgService;
    private final AuditService auditService;

    @Value("${summa.auth.jwt-secret}")
    private String jwtSecret;

    @Value("${summa.auth.jwt-expiration:86400000}")
    private long jwtExpiration;

    public AuthController(OrgService orgService, AuditService auditService) {
        this.orgService = orgService;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "code", "validation",
                "message", "email is required"
            ));
        }

        // Find human by email
        var humans = orgService.findAllHumans();
        var humanOpt = humans.stream()
            .filter(h -> email.equals(h.getEmail()))
            .findFirst();

        if (humanOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "code", "unauthorized",
                "message", "Invalid credentials"
            ));
        }

        var human = humanOpt.get();
        if (!human.isActive()) {
            return ResponseEntity.status(403).body(Map.of(
                "code", "forbidden",
                "message", "Account is deactivated"
            ));
        }

        if (password == null || password.isBlank()) {
            return ResponseEntity.status(401).body(Map.of(
                "code", "unauthorized",
                "message", "Invalid credentials"
            ));
        }

        String storedHash = human.getPasswordHash();
        if (storedHash == null || !PasswordUtil.verify(password, storedHash)) {
            return ResponseEntity.status(401).body(Map.of(
                "code", "unauthorized",
                "message", "Invalid credentials"
            ));
        }

        String token = JwtUtil.generateToken(human.getId(), jwtSecret, jwtExpiration);
        auditService.log(human.getId(), "LOGIN", "auth", human.getId(), null);

        return ResponseEntity.ok(Map.of(
            "token", token,
            "userId", human.getId(),
            "rbac", human.getRbac(),
            "name", human.getName()
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "summa"));
    }
}
