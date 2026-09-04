package com.summa.security;

import com.summa.service.MemberService;
import com.summa.model.Human;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
public class RbacAuthorizationFilter extends OncePerRequestFilter {

    private static final Map<String, String> WRITE_METHODS = Map.of(
        "POST", "write",
        "PUT", "write",
        "PATCH", "write",
        "DELETE", "write"
    );

    private static final ThreadLocal<String> ACTOR_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> WRITES_ALLOWED = new ThreadLocal<>();

    public static String getCurrentActor() {
        return ACTOR_CONTEXT.get();
    }

    public static String getCurrentActorOrDefault() {
        return ACTOR_CONTEXT.get() != null ? ACTOR_CONTEXT.get() : "system";
    }

    public static boolean isWriteAllowed() {
        Boolean val = WRITES_ALLOWED.get();
        return val != null && val;
    }

    private final MemberService memberService;

    public RbacAuthorizationFilter(MemberService memberService) {
        this.memberService = memberService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                      FilterChain filterChain) throws ServletException, IOException {
        // Public endpoints (health, login, bootstrap, ...) carry no actor by design.
        // JwtAuthenticationFilter already let them through — do not 401 them here,
        // otherwise /api/health breaks and fresh installs can never bootstrap.
        String path = request.getRequestURI();
        String normalized = path != null && path.endsWith("/") && path.length() > 1
                ? path.substring(0, path.length() - 1) : path;
        if (JwtAuthenticationFilter.PUBLIC_PATHS.contains(normalized)) {
            filterChain.doFilter(request, response);
            return;
        }
        String actor = (String) request.getAttribute("actor");
        if (actor == null) {
            // Do not trust X-Actor header from unauthenticated clients.
            // Only JWT-authenticated requests carry a valid actor via request attribute.
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No actor identity provided");
            return;
        }

        boolean writeAllowed = true;
        if (WRITE_METHODS.containsKey(request.getMethod())) {
            Optional<Human> humanOpt = memberService.findHuman(actor);
            if (humanOpt.isPresent()) {
                writeAllowed = memberService.hasWriteSurface(humanOpt.get());
            } else {
                var agentOpt = memberService.findAgent(actor);
                if (agentOpt.isPresent()) {
                    writeAllowed = memberService.hasWriteSurfaceAgent(agentOpt.get());
                } else {
                    writeAllowed = false;
                }
            }
        }

        try {
            ACTOR_CONTEXT.set(actor);
            WRITES_ALLOWED.set(writeAllowed);
            filterChain.doFilter(request, response);
        } finally {
            ACTOR_CONTEXT.remove();
            WRITES_ALLOWED.remove();
        }
    }
}
