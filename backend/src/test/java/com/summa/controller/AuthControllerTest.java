package com.summa.controller;

import com.summa.service.OrgService;
import com.summa.service.AuditService;
import com.summa.security.JwtUtil;
import com.summa.security.PasswordUtil;
import com.summa.security.PasswordValidator;
import com.summa.security.RateLimiter;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.model.Human;
import com.summa.model.AuditEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private OrgService orgService;
    private AuditService auditService;
    private PasswordUtil passwordUtil;
    private RateLimiter rateLimiter;
    private HttpServletRequest httpServletRequest;
    private AuthController controller;

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256";

    @BeforeEach
    void setUp() {
        orgService = mock(OrgService.class);
        auditService = mock(AuditService.class);
        passwordUtil = mock(PasswordUtil.class);
        rateLimiter = mock(RateLimiter.class);
        httpServletRequest = mock(HttpServletRequest.class);
        controller = new AuthController(orgService, auditService, passwordUtil, rateLimiter);
        // Inject test values via reflection for @Value fields
        setField(controller, "jwtSecret", SECRET);
        setField(controller, "jwtExpiration", 3600_000L);
        setField(controller, "minJwtSecretLength", 16);
        setField(controller, "localAuthEnabled", true);
        setField(controller, "trustedProxyIps", "");
        // Default stub: any auditService call returns a valid event to prevent NPE
        when(auditService.logSystem(anyString(), anyString(), any(), any()))
            .thenReturn(newAuditEvent());
        when(auditService.log(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(newAuditEvent());
        // Stub the 5-arg overload explicitly (it delegates internally but Mockito tracks calls separately)
        doReturn(newAuditEvent()).when(auditService).log(any(), any(), any(), any(), any());
        when(rateLimiter.allow(anyString())).thenReturn(true);
        when(rateLimiter.getRemainingAttempts(anyString())).thenReturn(5L);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        RbacAuthorizationFilter.setActorForTest("user-1");
    }

    @Test
    void login_success_returnsToken() {
        Human human = newHuman();
        human.setPasswordHash("$2a$12$hash");
        when(orgService.findHumanByEmail("test@example.com")).thenReturn(Optional.of(human));
        when(passwordUtil.verify("password123!", "$2a$12$hash")).thenReturn(true);
        when(rateLimiter.allow(eq("test@example.com:127.0.0.1"))).thenReturn(true);

        Map<String, String> body = Map.of("email", "test@example.com", "password", "password123!");
        var result = controller.login(body, httpServletRequest);

        assertEquals(200, result.getStatusCodeValue());
        assertNotNull(result.getBody().get("token"));
        assertEquals("user-1", result.getBody().get("userId"));
        assertEquals("member", result.getBody().get("rbac"));
        assertEquals("Test User", result.getBody().get("name"));
        verify(rateLimiter).reset(eq("test@example.com:127.0.0.1"));
    }

    @Test
    void login_blankEmail_returnsValidationError() {
        Map<String, String> body = Map.of("email", "", "password", "pass");
        var result = controller.login(body, httpServletRequest);

        assertEquals(422, result.getStatusCodeValue());
        assertEquals("validation", result.getBody().get("code"));
    }

    @Test
    void login_invalidEmailFormat_returnsValidationError() {
        Map<String, String> body = Map.of("email", "notanemail", "password", "pass");
        var result = controller.login(body, httpServletRequest);

        assertEquals(422, result.getStatusCodeValue());
    }

    @Test
    void login_unknownAccount_returnsGate() {
        when(orgService.findHumanByEmail("unknown@example.com")).thenReturn(Optional.empty());

        Map<String, String> body = Map.of("email", "unknown@example.com", "password", "pass");
        var result = controller.login(body, httpServletRequest);

        assertEquals(403, result.getStatusCodeValue());
        assertEquals("gate", result.getBody().get("code"));
    }

    @Test
    void login_wrongPassword_returnsGate() {
        Human human = newHuman();
        human.setPasswordHash("$2a$12$hash");
        when(orgService.findHumanByEmail("test@example.com")).thenReturn(Optional.of(human));
        when(passwordUtil.verify("wrong", "$2a$12$hash")).thenReturn(false);

        Map<String, String> body = Map.of("email", "test@example.com", "password", "wrong");
        var result = controller.login(body, httpServletRequest);

        assertEquals(403, result.getStatusCodeValue());
        assertEquals("gate", result.getBody().get("code"));
    }

    @Test
    void login_rateLimited_returnsTooManyRequests() {
        when(rateLimiter.allow(anyString())).thenReturn(false);
        when(rateLimiter.getRemainingAttempts(anyString())).thenReturn(2L);

        Map<String, String> body = Map.of("email", "test@example.com", "password", "pass");
        var result = controller.login(body, httpServletRequest);

        assertEquals(429, result.getStatusCodeValue());
        assertEquals("rate_limited", result.getBody().get("code"));
        assertEquals(2L, result.getBody().get("remainingAttempts"));
    }

    @Test
    void changePassword_success_updatesSessionVersion() {
        Human human = newHuman();
        human.setPasswordHash("$2a$12$hash");
        human.setSessionVersion(1);
        when(orgService.findHumanForUpdate("user-1")).thenReturn(Optional.of(human));
        when(passwordUtil.verify("oldpass", "$2a$12$hash")).thenReturn(true);
        when(passwordUtil.hash("Newpass1234!")).thenReturn("$2a$12$newhash");
        when(rateLimiter.allow(anyString())).thenReturn(true);

        Map<String, String> body = Map.of("currentPassword", "oldpass", "newPassword", "Newpass1234!");
        var result = controller.changePassword(body, httpServletRequest);

        assertEquals(200, result.getStatusCodeValue());
        assertEquals("Password updated", result.getBody().get("message"));
        assertEquals(2, human.getSessionVersion());
        verify(orgService).saveHuman(human);
    }

    @Test
    void changePassword_wrongCurrentPassword_returnsGate() {
        Human human = newHuman();
        human.setPasswordHash("$2a$12$hash");
        when(orgService.findHumanForUpdate("user-1")).thenReturn(Optional.of(human));
        when(passwordUtil.verify("wrong", "$2a$12$hash")).thenReturn(false);
        when(rateLimiter.allow(anyString())).thenReturn(true);

        Map<String, String> body = Map.of("currentPassword", "wrong", "newPassword", "Newpass1234!");
        var result = controller.changePassword(body, httpServletRequest);

        assertEquals(403, result.getStatusCodeValue());
        assertEquals("gate", result.getBody().get("code"));
    }

    @Test
    void changePassword_weakNewPassword_returnsValidationError() {
        when(orgService.findHumanForUpdate("user-1")).thenReturn(Optional.of(newHuman()));
        when(rateLimiter.allow(anyString())).thenReturn(true);

        Map<String, String> body = Map.of("currentPassword", "oldpass", "newPassword", "weak");
        var result = controller.changePassword(body, httpServletRequest);

        assertEquals(422, result.getStatusCodeValue());
        assertEquals("validation", result.getBody().get("code"));
    }

    @Test
    void changePassword_rateLimited_returnsTooManyRequests() {
        when(rateLimiter.allow(anyString())).thenReturn(false);
        when(rateLimiter.getRemainingAttempts(anyString())).thenReturn(3L);

        Map<String, String> body = Map.of("currentPassword", "old", "newPassword", "new!");
        var result = controller.changePassword(body, httpServletRequest);

        assertEquals(429, result.getStatusCodeValue());
    }

    @Test
    void login_localAuthDisabled_returnsGateRefusal() {
        setField(controller, "localAuthEnabled", false);

        Map<String, String> body = Map.of("email", "test@example.com", "password", "pass");
        var result = controller.login(body, httpServletRequest);

        assertEquals(403, result.getStatusCodeValue());
        assertEquals("gate", result.getBody().get("code"));
    }

    private Human newHuman() {
        Human h = new Human();
        h.setId("user-1");
        h.setEmail("test@example.com");
        h.setName("Test User");
        h.setRbac("member");
        h.setSessionVersion(1);
        return h;
    }

    private AuditEvent newAuditEvent() {
        AuditEvent e = new AuditEvent();
        e.setId("audit-123");
        return e;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }
}
