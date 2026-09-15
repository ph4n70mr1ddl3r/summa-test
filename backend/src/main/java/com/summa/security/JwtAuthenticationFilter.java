package com.summa.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void validateSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("summa.auth.jwt-secret must not be blank");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("summa.auth.jwt-secret must be at least 32 characters (256 bits recommended), got " + jwtSecret.length());
        }
    }

    public static final List<String> PUBLIC_PATHS = List.of(
        "/api/auth/login", "/api/health", "/api/info",
        "/api/nodes/enroll", "/api/org/bootstrap",
        "/api/nodes/*/heartbeat", "/api/nodes/*/claims",
        "/api/nodes/*/work/pull", "/api/nodes/*/runs/*/report"
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                      @NonNull HttpServletResponse response,
                                      @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Map<String, Object> payload = JwtUtil.parseToken(token, jwtSecret);
            if (payload != null) {
                String subject = (String) payload.get("sub");
                request.setAttribute("authSubject", subject);
                request.setAttribute("actor", subject);
                filterChain.doFilter(request, response);
                return;
            }
            log.warn("[SUMMA] invalid/expired JWT from {} path={}", request.getRemoteAddr(), path);
        }

        // Reject requests without valid JWT
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid authentication");
        return;
    }

    private boolean isPublicPath(String path) {
        if (path == null) return false;
        if (PUBLIC_PATHS.contains(path)) return true;
        // Tolerate a trailing slash (e.g. /api/health/) without opening prefixes.
        if (path.endsWith("/") && path.length() > 1) {
            if (PUBLIC_PATHS.contains(path.substring(0, path.length() - 1))) return true;
        }
        // Match node API wildcard paths (e.g. /api/nodes/<uuid>/heartbeat)
        for (String pattern : PUBLIC_PATHS) {
            if (pattern.contains("*")) {
                String prefix = pattern.substring(0, pattern.indexOf('*'));
                if (!path.startsWith(prefix)) continue;
                String suffix = pattern.substring(pattern.indexOf('*') + 1);
                String remainder = path.substring(prefix.length());
                if (suffix.isEmpty()) return true;
                if (!remainder.startsWith("/")) continue;
                String[] parts = remainder.split("/");
                if (suffix.startsWith("/")) {
                    String suffixWithoutLeadingSlash = suffix.substring(1);
                    String[] suffixParts = suffixWithoutLeadingSlash.isEmpty() ? new String[0] : suffixWithoutLeadingSlash.split("/");
                    if (parts.length < suffixParts.length) continue;
                    for (int i = 0; i < suffixParts.length; i++) {
                        if (!suffixParts[i].equals("*") && !parts[parts.length - suffixParts.length + i].equals(suffixParts[i])) {
                            continue;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
