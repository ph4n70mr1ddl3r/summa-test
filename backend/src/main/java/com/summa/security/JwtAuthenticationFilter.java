package com.summa.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        "/api/nodes/enroll"
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
        }

        // Reject requests without valid JWT
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid authentication");
        return;
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.equals(publicPath) || path.startsWith(publicPath + "/")) {
                return true;
            }
        }
        return false;
    }
}
