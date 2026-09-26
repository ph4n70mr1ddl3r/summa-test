package com.summa.controller;

import com.summa.service.OrgService;
import com.summa.service.AuditService;
import com.summa.security.JwtUtil;
import com.summa.security.PasswordUtil;
import com.summa.security.PasswordValidator;
import com.summa.security.RateLimiter;
import com.summa.security.RbacAuthorizationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final OrgService orgService;
    private final AuditService auditService;
    private final PasswordUtil passwordUtil;
    private final RateLimiter rateLimiter;

    @Value("${summa.auth.local-auth-enabled:true}")
    private boolean localAuthEnabled;

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
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        if (!localAuthEnabled) {
            return ControllerResponses.serviceUnavailable(auditService, "Local authentication is not enabled. Use OIDC/gateway auth instead.");
        }

        String email = body.get("email");
        String password = body.get("password");

        if (email == null || email.isBlank()) {
            return ControllerResponses.validation(auditService, "email is required");
        }
        if (!email.matches("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")) {
            return ControllerResponses.validation(auditService, "email has invalid format: " + email);
        }

        // Rate limit by email+IP to prevent cross-user DoS via email-based keying.
        String clientIp = resolveClientIp(request);
        String rateKey = email + ":" + clientIp;
        if (!rateLimiter.allow(rateKey)) {
            long remaining = rateLimiter.getRemainingAttempts(rateKey);
            var audit = auditService.logSystem("REFUSAL", "auth_login", email, "Rate limited login attempt for: " + email);
            return ControllerResponses.tooManyRequests(audit, "Too many login attempts. Try again later.", remaining);
        }

        var humanOpt = orgService.findHumanByEmail(email);

        if (humanOpt.isEmpty()) {
            var audit = auditService.logSystem("REFUSAL", "auth_login", email, "Login attempt for unknown account");
            return ControllerResponses.gate(audit, "Invalid credentials");
        }
        if (!humanOpt.get().isActive()) {
            var audit = auditService.logSystem("REFUSAL", "auth_login", email, "Login attempt on deactivated account");
            return ControllerResponses.gate(audit, "Invalid credentials");
        }

        var human = humanOpt.get();

        if (password == null || password.isBlank() || human.getPasswordHash() == null
                || !passwordUtil.verify(password, human.getPasswordHash())) {
            var audit = auditService.logSystem("REFUSAL", "auth_login", email, "Login attempt with bad password");
            return ControllerResponses.gate(audit, "Invalid credentials");
        }

        String token = JwtUtil.generateToken(human.getId(), jwtSecret, jwtExpiration);
        auditService.log(human.getId(), "LOGIN", "auth", human.getId(), null);
        // Reset rate limit counter on successful login
        rateLimiter.reset(rateKey);

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
        // RbacAuthorizationFilter already ran before this controller and set the actor
        // attribute. Re-parse here only to satisfy the explicit auth header check;
        // in practice the filter chain guarantees a valid actor is available.
        String actor = RbacAuthorizationFilter.getCurrentActor();
        if (actor == null || actor.isBlank()) {
            var audit = auditService.logSystem("REFUSAL", "auth_change_password", "Missing token", null);
            return ControllerResponses.gate(audit, "Missing or malformed Authorization header");
        }

        // Rate limit by actor to prevent brute-force password changes
        if (!rateLimiter.allow(actor + ":change-password")) {
            long remaining = rateLimiter.getRemainingAttempts(actor + ":change-password");
            var audit = auditService.logSystem("REFUSAL", "auth_change_password", "Rate limited password change for: " + actor, null);
            return ControllerResponses.tooManyRequests(audit, "Too many password change attempts. Try again later.", remaining);
        }

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || currentPassword.isBlank()) {
            return ControllerResponses.validation(auditService, "currentPassword is required");
        }
        if (newPassword == null || newPassword.isBlank()) {
            return ControllerResponses.validation(auditService, "newPassword is required");
        }
        try {
            PasswordValidator.validate(newPassword);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        }

        // Re-fetch under pessimistic lock to prevent concurrent password changes
        // from both passing the old-password check and overwriting each other.
        var humanOpt = orgService.findHumanForUpdate(actor);
        if (humanOpt.isEmpty()) {
            var audit = auditService.logSystem("REFUSAL", "auth_change_password", "Invalid credentials for: " + actor, null);
            return ControllerResponses.gate(audit, "Invalid credentials");
        }
        var human = humanOpt.get();

        if (human.getPasswordHash() == null || !passwordUtil.verify(currentPassword, human.getPasswordHash())) {
            var audit = auditService.logSystem("REFUSAL", "auth_change_password", "Password mismatch for: " + actor, null);
            return ControllerResponses.gate(audit, "Current password is incorrect");
        }

        human.setPasswordHash(passwordUtil.hash(newPassword));
        orgService.saveHuman(human);
        auditService.log(actor, "CHANGE_PASSWORD", "auth", actor, null);

        return ResponseEntity.ok(Map.of("message", "Password updated"));
    }

    private boolean isTrustedProxy(String addr) {
        if ("local".equals(addr) || "127.0.0.1".equals(addr) || "0:0:0:0:0:0:0:1".equals(addr)) {
            return false;
        }
        // RFC1918 private ranges: 10.0.0.0/8, 172.16.0.0/12 (172.16-31.x.x), 192.168.0.0/16
        if (addr.startsWith("10.")) return true;
        if (addr.startsWith("192.168.")) return true;
        if (addr.startsWith("172.")) {
            // Extract second octet for 172.16.0.0/12 check
            int dot1 = addr.indexOf('.', 4);
            if (dot1 > 4) {
                try {
                    int secondOctet = Integer.parseInt(addr.substring(4, dot1));
                    return secondOctet >= 16 && secondOctet <= 31;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return false;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String xfwd = request.getHeader("X-Forwarded-For");
            if (xfwd != null && !xfwd.isBlank()) {
                return xfwd.split(",")[0].trim();
            }
        }
        return "local".equals(remoteAddr) ? "127.0.0.1" : remoteAddr;
    }
}
