package com.summa.security;

import com.summa.constants.Defaults;
import com.summa.service.MemberService;
import com.summa.service.AuditService;
import com.summa.model.Human;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
public class RbacAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RbacAuthorizationFilter.class);

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

    /** Package-private helpers for unit testing. */
    static void clearThreadLocals() {
        ACTOR_CONTEXT.remove();
        WRITES_ALLOWED.remove();
        NODE_AUTH.remove();
    }

    static void setNodeAuthForTest(boolean value) {
        NODE_AUTH.set(value);
    }

    public static void setActorForTest(String actor) {
        ACTOR_CONTEXT.set(actor);
    }

    private final MemberService memberService;
    private final AuditService auditService;

    public RbacAuthorizationFilter(MemberService memberService, AuditService auditService) {
        this.memberService = memberService;
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                      FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String normalized = com.summa.util.JsonHelpers.normalizeTrailingSlash(path);

        String actor = (String) request.getAttribute("actor");
        boolean nodeAuth = Boolean.TRUE.equals(request.getAttribute("nodeAuth"));
        boolean isPublic = isPublicPath(normalized);

        // Always ensure ThreadLocals are cleared at the start of each request
        // to prevent leakage from a previous request that failed to clean up.
        ACTOR_CONTEXT.remove();
        WRITES_ALLOWED.remove();
        NODE_AUTH.remove();

        if (!isPublic) {
            if (actor == null) {
                writeRefusal(response, HttpServletResponse.SC_UNAUTHORIZED, "No actor identity provided");
                ACTOR_CONTEXT.remove();
                WRITES_ALLOWED.remove();
                NODE_AUTH.remove();
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

    private boolean isPublicPath(String path) {
        return JwtAuthenticationFilter.isPublicPath(path);
    }

    /**
     * Writes a structured JSON refusal response per API-061, including an audit event.
     */
    private void writeRefusal(HttpServletResponse response, int status, String message) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        com.summa.model.AuditEvent auditEvent = auditService.logSystem("REFUSAL", "authz", null, message);
        Map<String, Object> body = Map.of(
            "code", "unauthorized",
            "message", message,
            "audit_event_id", auditEvent.getId()
        );
        response.setStatus(status);
        response.getWriter().write(com.summa.util.JsonHelpers.toJson(body, null));
    }
}
