package com.summa.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.summa.service.AuditService;
import com.summa.repository.HumanRepository;
import com.summa.model.Human;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${summa.auth.jwt-secret}")
    private String jwtSecret;

    @Value("${summa.auth.jwt-secret-min-length:32}")
    private int minJwtSecretLength;

    private final HumanRepository humanRepository;
    private final AuditService auditService;

    @PostConstruct
    public void validateSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("summa.auth.jwt-secret must not be blank");
        }
        if (jwtSecret.length() < minJwtSecretLength) {
            throw new IllegalStateException("summa.auth.jwt-secret must be at least " + minJwtSecretLength + " characters (256 bits recommended), got " + jwtSecret.length());
        }
    }

    public JwtAuthenticationFilter(HumanRepository humanRepository, AuditService auditService) {
        this.humanRepository = humanRepository;
        this.auditService = auditService;
    }

    public static final List<String> PUBLIC_PATHS = List.of(
        "/api/auth/login", "/api/health", "/api/info",
        "/api/nodes/enroll", "/api/org/bootstrap"
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                      @NonNull HttpServletResponse response,
                                      @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isPublicPath(path)) {
            // For public paths, parse JWT if present so downstream filters can set up
            // actor context for authenticated callers (e.g. admin enrolling nodes or
            // re-bootstrapping). Unauthenticated calls still pass through cleanly.
            // SEC-010: Session version is always checked even on public paths to prevent
            // stale tokens from carrying actor context after password changes.
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                if (authHeader.length() > 7) {
                    String token = authHeader.substring(7);
                    Map<String, Object> payload = JwtUtil.parseToken(token, jwtSecret);
                    if (payload != null) {
                        String subject = payload.get("sub") instanceof String s ? s : null;
                        if (subject != null && !subject.isBlank()) {
                            if (!checkSessionVersion(subject, payload)) {
                                log.warn("[SUMMA] Stale session version on public path={}", path);
                                writeRefusal(response, HttpServletResponse.SC_UNAUTHORIZED, "Session invalidated — please sign in again", subject);
                                return;
                            }
                            request.setAttribute("actor", subject);
                            var auth = new UsernamePasswordAuthenticationToken(subject, null, List.of());
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    } else {
                        log.warn("[SUMMA] invalid/expired JWT on public path from {} path={}", request.getRemoteAddr(), path);
                    }
                }
            }
            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
            return;
        }

        // Node callbacks (API-060) are authenticated by NodeAuthFilter, which has
        // already installed the node principal in the SecurityContext — preserve it.
        if (Boolean.TRUE.equals(request.getAttribute("nodeAuth"))) {
            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            if (authHeader.length() > 7) {
                String token = authHeader.substring(7);
                Map<String, Object> payload = JwtUtil.parseToken(token, jwtSecret);
                if (payload != null) {
                    String subject = payload.get("sub") instanceof String s ? s : null;
                    if (subject == null || subject.isBlank()) {
                        log.warn("[SUMMA] JWT missing valid sub claim from {} path={}", request.getRemoteAddr(), path);
                        writeRefusal(response, HttpServletResponse.SC_UNAUTHORIZED, "JWT missing valid subject", request.getRemoteAddr());
                        return;
                    }
                    // SEC-010: Verify session version matches current human record
                    // This invalidates tokens after password changes or session resets
                    if (!checkSessionVersion(subject, payload)) {
                        log.warn("[SUMMA] Stale session version on path={}", path);
                        writeRefusal(response, HttpServletResponse.SC_UNAUTHORIZED, "Session invalidated — please sign in again", subject);
                        return;
                    }
                    request.setAttribute("actor", subject);
                    var auth = new UsernamePasswordAuthenticationToken(subject, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    try {
                        filterChain.doFilter(request, response);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                    return;
                }
                log.warn("[SUMMA] invalid/expired JWT from {} path={}", request.getRemoteAddr(), path);
            }
        }

        // Reject requests without valid JWT
        writeRefusal(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid authentication", null);
        return;
    }

    /** SEC-010: Verify the JWT session version matches the human's current session version. */
    private boolean checkSessionVersion(String subject, Map<String, Object> payload) {
        Object svObj = payload.get("sv");
        long tokenSv;
        if (svObj instanceof Number num) {
            long raw = num.longValue();
            if (raw < 0 || raw > Integer.MAX_VALUE) return false;
            tokenSv = raw;
        } else {
            tokenSv = 0;
        }
        try {
            Optional<Human> humanOpt = humanRepository.findById(subject);
            if (humanOpt.isEmpty()) return false;
            return tokenSv == humanOpt.get().getSessionVersion();
        } catch (Exception e) {
            log.warn("[SUMMA] Session version check failed for {}: {}", subject, e.getMessage());
            return false;
        }
    }

    static boolean isPublicPath(String path) {
        if (path == null) return false;
        if (PUBLIC_PATHS.contains(path)) return true;
        // Tolerate a trailing slash (e.g. /api/health/) without opening prefixes.
        String normalized = com.summa.util.JsonHelpers.normalizeTrailingSlash(path);
        if (normalized != null) {
            return PUBLIC_PATHS.contains(normalized);
        }
        return false;
    }

    /**
     * Writes a structured JSON refusal response per API-061, including an audit event.
     */
    private void writeRefusal(HttpServletResponse response, int status, String message, String actor)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        com.summa.model.AuditEvent auditEvent = auditService.logSystem("REFUSAL", "auth", null, message);
        Map<String, Object> body = java.util.Map.of(
            "code", "unauthorized",
            "message", message,
            "audit_event_id", auditEvent.getId()
        );
        response.setStatus(status);
        response.getWriter().write(com.summa.util.JsonHelpers.toJson(body, null));
    }
}
