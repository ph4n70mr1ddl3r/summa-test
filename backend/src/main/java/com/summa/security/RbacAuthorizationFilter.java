package com.summa.security;

import com.summa.constants.Defaults;
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
    private static final ThreadLocal<Boolean> NODE_AUTH = new ThreadLocal<>();

    public static String getCurrentActor() {
        return ACTOR_CONTEXT.get();
    }

    public static String getCurrentActorOrDefault() {
        String actor = ACTOR_CONTEXT.get();
        return actor != null ? actor : Defaults.SYSTEM_ACTOR;
    }

    public static boolean isWriteAllowed() {
        Boolean val = WRITES_ALLOWED.get();
        return val != null && val;
    }

    public static Boolean getNodeAuth() {
        return NODE_AUTH.get();
    }

    private final MemberService memberService;

    public RbacAuthorizationFilter(MemberService memberService) {
        this.memberService = memberService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                      FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String normalized = path != null && path.endsWith("/") && path.length() > 1
                ? path.substring(0, path.length() - 1) : path;

        String actor = (String) request.getAttribute("actor");
        boolean nodeAuth = Boolean.TRUE.equals(request.getAttribute("nodeAuth"));
        boolean isPublic = JwtAuthenticationFilter.PUBLIC_PATHS.contains(normalized);

        if (!isPublic) {
            if (actor == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No actor identity provided");
                return;
            }
        }

        String effectiveActor = actor != null ? actor : Defaults.SYSTEM_ACTOR;
        boolean writeAllowed = resolveWriteAllowed(actor, nodeAuth);

        ACTOR_CONTEXT.set(effectiveActor);
        WRITES_ALLOWED.set(writeAllowed);
        NODE_AUTH.set(nodeAuth);
        try {
            filterChain.doFilter(request, response);
        } finally {
            ACTOR_CONTEXT.remove();
            WRITES_ALLOWED.remove();
            NODE_AUTH.remove();
        }
    }

    private boolean resolveWriteAllowed(String actor, boolean nodeAuth) {
        if (nodeAuth) return true;
        if (actor == null) return false;
        Optional<Human> humanOpt = memberService.findHuman(actor);
        if (humanOpt.isPresent()) {
            return memberService.hasWriteSurface(humanOpt.get());
        }
        var agentOpt = memberService.findAgent(actor);
        return agentOpt.isPresent() && memberService.hasWriteSurfaceAgent(agentOpt.get());
    }
}
