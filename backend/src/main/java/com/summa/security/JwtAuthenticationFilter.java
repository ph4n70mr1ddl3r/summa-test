package com.summa.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${summa.auth.jwt-secret}")
    private String jwtSecret;

    @Value("${summa.auth.jwt-expiration:86400000}")
    private long jwtExpiration;

    @Value("${summa.auth.jwt-secret-min-length:32}")
    private int minJwtSecretLength;

    @PostConstruct
    public void validateSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("summa.auth.jwt-secret must not be blank");
        }
        if (jwtSecret.length() < minJwtSecretLength) {
            throw new IllegalStateException("summa.auth.jwt-secret must be at least " + minJwtSecretLength + " characters (256 bits recommended), got " + jwtSecret.length());
        }
    }

    public static final List<String> PUBLIC_PATHS = List.of(
        "/api/auth/login", "/api/health", "/api/info",
        "/api/nodes/enroll", "/api/org/bootstrap"
    );

    public static boolean isNodePath(String path) {
        if (path == null) return false;
        // Tolerate a trailing slash (e.g. /api/nodes/)
        String normalizedPath = com.summa.util.JsonHelpers.normalizeTrailingSlash(path);
        return normalizedPath != null && normalizedPath.startsWith("/api/nodes");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                      @NonNull HttpServletResponse response,
                                      @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isPublicPath(path)) {
            // For public paths, parse JWT if present so downstream filters can set up
            // actor context for authenticated callers (e.g. admin enrolling nodes or
            // re-bootstrapping). Unauthenticated calls still pass through cleanly.
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                if (authHeader.length() > 7) {
                    String token = authHeader.substring(7);
                    Map<String, Object> payload = JwtUtil.parseToken(token, jwtSecret);
                    if (payload != null) {
                        String subject = payload.get("sub") instanceof String s ? s : null;
                        if (subject != null && !subject.isBlank()) {
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

        if (isNodePath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // If node auth already authenticated this request, skip JWT check
        if (Boolean.TRUE.equals(request.getAttribute("nodeAuth"))) {
            filterChain.doFilter(request, response);
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
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT missing valid subject");
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
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid authentication");
        return;
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
}
