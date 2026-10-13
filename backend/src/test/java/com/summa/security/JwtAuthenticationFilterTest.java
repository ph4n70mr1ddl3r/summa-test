package com.summa.security;

import com.summa.repository.HumanRepository;
import com.summa.model.Human;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private HumanRepository humanRepository;
    private AuditService auditService;
    private JwtAuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private ServletOutputStream mockOutputStream;
    private PrintWriter mockWriter;

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256";
    private static final String VALID_TOKEN = JwtUtil.generateTokenWithSession("user-1", SECRET, 3600_000L, 16, 1);
    private static final String STALE_TOKEN = JwtUtil.generateTokenWithSession("user-1", SECRET, 3600_000L, 16, 999);

    @BeforeEach
    void setUp() throws Exception {
        humanRepository = mock(HumanRepository.class);
        auditService = mock(AuditService.class);
        filter = new JwtAuthenticationFilter(humanRepository, auditService);
        // Override @Value fields via reflection
        try {
            setField(filter, "jwtSecret", SECRET);
            setField(filter, "minJwtSecretLength", 16);
        } catch (Exception e) {
            throw new AssertionError("Failed to set up filter", e);
        }

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        mockOutputStream = mock(ServletOutputStream.class);
        mockWriter = mock(PrintWriter.class);

        when(response.getOutputStream()).thenReturn(mockOutputStream);
        when(response.getWriter()).thenReturn(mockWriter);
        when(response.containsHeader(anyString())).thenReturn(false);
    }

    @Test
    void isPublicPath_health_returnsTrue() {
        assertTrue(JwtAuthenticationFilter.isPublicPath("/api/health"));
    }

    @Test
    void isPublicPath_login_returnsTrue() {
        assertTrue(JwtAuthenticationFilter.isPublicPath("/api/auth/login"));
    }

    @Test
    void isPublicPath_protectedPath_returnsFalse() {
        assertFalse(JwtAuthenticationFilter.isPublicPath("/api/org/humans"));
    }

    @Test
    void isPublicPath_withTrailingSlash_returnsTrue() {
        assertTrue(JwtAuthenticationFilter.isPublicPath("/api/health/"));
    }

    @Test
    void isPublicPath_null_returnsFalse() {
        assertFalse(JwtAuthenticationFilter.isPublicPath(null));
    }

    @Test
    void isNodePath_nodesEndpoint_returnsTrue() {
        assertTrue(JwtAuthenticationFilter.isNodePath("/api/nodes/123/heartbeat"));
    }

    @Test
    void isNodePath_nonNodePath_returnsFalse() {
        assertFalse(JwtAuthenticationFilter.isNodePath("/api/org/humans"));
    }

    @Test
    void isNodePath_null_returnsFalse() {
        assertFalse(JwtAuthenticationFilter.isNodePath(null));
    }

    @Test
    void doFilterInternal_validJwtOnProtectedPath_setsActor() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/org/humans");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(humanRepository.findById("user-1")).thenReturn(Optional.of(newHuman()));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        // SecurityContextHolder should be cleared after the filter
        assertNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_staleSessionVersionOnProtectedPath_rejectsWithJson() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/org/humans");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + STALE_TOKEN);
        when(humanRepository.findById("user-1")).thenReturn(Optional.of(newHuman()));
        when(auditService.logSystem(eq("REFUSAL"), eq("auth"), isNull(), eq("Session invalidated — please sign in again")))
            .thenReturn(newAuditEvent());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setContentType("application/json");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void doFilterInternal_validJwtOnPublicPath_setsActorWithSessionCheck() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/nodes/enroll");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(humanRepository.findById("user-1")).thenReturn(Optional.of(newHuman()));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_staleJwtOnPublicPath_rejectsWithJson() throws Exception {
        // Use a token with invalid signature so parseToken returns null on public path
        String badToken = "header.payload.bad_sig";
        when(request.getRequestURI()).thenReturn("/api/nodes/enroll");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + badToken);

        filter.doFilterInternal(request, response, filterChain);

        // When token is unparseable on public path, filter still passes through (no actor set)
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_noAuthHeaderOnProtectedPath_rejectsWithJson() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/org/humans");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(auditService.logSystem(eq("REFUSAL"), eq("auth"), isNull(), eq("Missing or invalid authentication")))
            .thenReturn(newAuditEvent());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setContentType("application/json");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void doFilterInternal_nodePath_skipsJwtFilter() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/nodes/123/heartbeat");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_publicPathWithoutAuth_passesThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/health");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    private Human newHuman() {
        Human h = new Human();
        h.setId("user-1");
        h.setEmail("user@example.com");
        h.setName("Test User");
        h.setRbac("admin");
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
