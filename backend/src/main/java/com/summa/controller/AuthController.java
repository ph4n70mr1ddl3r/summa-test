package com.summa.controller;

import com.summa.service.OrgService;
import com.summa.service.AuditService;
import com.summa.security.JwtUtil;
import com.summa.security.PasswordUtil;
import com.summa.security.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final OrgService orgService;
    private final AuditService auditService;
    private final PasswordUtil passwordUtil;
    private final RateLimiter rateLimiter;

    @Value("${summa.auth.jwt-secret}")
    private String jwtSecret;

    @Value("${summa.auth.jwt-expiration:86400000}")
    private long jwtExpiration;

    public AuthController(OrgService orgService, AuditService auditService, PasswordUtil passwordUtil, RateLimiter rateLimiter) {
        this.orgService = orgService;
        this.auditService = auditService;
        this.passwordUtil = passwordUtil;
        this.rateLimiter = rateLimiter;
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

        // Rate limit by email to prevent brute-force
        if (!rateLimiter.allow(email)) {
            long remaining = rateLimiter.getRemainingAttempts(email);
            var audit = auditService.logSystem("REFUSAL", "auth_login", "Rate limited login attempt for: " + email, null);
            return ResponseEntity.status(429).body(Map.of(
                "code", "rate_limited",
                "message", "Too many login attempts. Try again later.",
                "audit_event_id", audit.getId(),
                "remainingAttempts", remaining
            ));
        }

        var humanOpt = orgService.findHumanByEmail(email);

        if (humanOpt.isEmpty()) {
            var audit = auditService.logSystem("REFUSAL", "auth_login", "Login attempt for unknown account", null);
            return ResponseEntity.status(401).body(Map.of(
                "code", "unauthorized",
                "message", "Invalid credentials",
                "audit_event_id", audit.getId()
            ));
        }
        if (!humanOpt.get().isActive()) {
            var audit = auditService.logSystem("REFUSAL", "auth_login", "Login attempt on deactivated account", null);
            return ResponseEntity.status(401).body(Map.of(
                "code", "unauthorized",
                "message", "Invalid credentials",
                "audit_event_id", audit.getId()
            ));
        }

        var human = humanOpt.get();

        if (password == null || password.isBlank() || human.getPasswordHash() == null
                || !passwordUtil.verify(password, human.getPasswordHash())) {
            var audit = auditService.logSystem("REFUSAL", "auth_login", "Login attempt with bad password", null);
            return ResponseEntity.status(401).body(Map.of(
                "code", "unauthorized",
                "message", "Invalid credentials",
                "audit_event_id", audit.getId()
            ));
        }

        String token = JwtUtil.generateToken(human.getId(), jwtSecret, jwtExpiration);
        auditService.log(human.getId(), "LOGIN", "auth", human.getId(), null);
        // Reset rate limit counter on successful login
        rateLimiter.allow(human.getId() + ":reset");

        return ResponseEntity.ok(Map.of(
            "token", token,
            "userId", human.getId(),
            "rbac", human.getRbac(),
            "name", human.getName()
        ));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestHeader(value = "Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        String token = extractToken(authHeader);
        if (token == null) {
            var audit = auditService.logSystem("REFUSAL", "auth_change_password", "Missing token", null);
            return ResponseEntity.status(401).body(Map.of("code", "unauthorized", "message", "Missing token", "audit_event_id", audit.getId()));
        }
        var payload = JwtUtil.parseToken(token, jwtSecret);
        if (payload == null) {
            var audit = auditService.logSystem("REFUSAL", "auth_change_password", "Invalid token", null);
            return ResponseEntity.status(401).body(Map.of("code", "unauthorized", "message", "Invalid token", "audit_event_id", audit.getId()));
        }
        String actor = (String) payload.get("sub");

        // Rate limit by actor to prevent brute-force password changes
        if (!rateLimiter.allow(actor + ":change-password")) {
            long remaining = rateLimiter.getRemainingAttempts(actor + ":change-password");
            var audit = auditService.logSystem("REFUSAL", "auth_change_password", "Rate limited password change for: " + actor, null);
            return ResponseEntity.status(429).body(Map.of(
                "code", "rate_limited",
                "message", "Too many password change attempts. Try again later.",
                "audit_event_id", audit.getId(),
                "remainingAttempts", remaining
            ));
        }

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || currentPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("code", "validation", "message", "currentPassword is required"));
        }
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("code", "validation", "message", "newPassword is required"));
        }
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("code", "validation", "message", "newPassword must be at least 8 characters"));
        }

        var humanOpt = orgService.findHuman(actor);
        if (humanOpt.isEmpty()) {
            var audit = auditService.logSystem("REFUSAL", "auth_change_password", "Invalid credentials for: " + actor, null);
            return ResponseEntity.status(401).body(Map.of("code", "unauthorized", "message", "Invalid credentials", "audit_event_id", audit.getId()));
        }
        var human = humanOpt.get();

        if (human.getPasswordHash() == null || !passwordUtil.verify(currentPassword, human.getPasswordHash())) {
            var audit = auditService.logSystem("REFUSAL", "auth_change_password", "Password mismatch for: " + actor, null);
            return ResponseEntity.status(401).body(Map.of("code", "unauthorized", "message", "Invalid credentials", "audit_event_id", audit.getId()));
        }

        human.setPasswordHash(passwordUtil.hash(newPassword));
        orgService.saveHuman(human);
        auditService.log(actor, "CHANGE_PASSWORD", "auth", actor, null);

        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
