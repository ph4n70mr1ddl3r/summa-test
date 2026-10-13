package com.summa.security;

import com.summa.model.Human;
import com.summa.model.Agent;
import com.summa.service.MemberService;
import com.summa.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RbacAuthorizationFilterTest {

    private MemberService memberService;
    private AuditService auditService;
    private RbacAuthorizationFilter filter;

    @BeforeEach
    void setUp() {
        memberService = mock(MemberService.class);
        auditService = mock(AuditService.class);
        filter = new RbacAuthorizationFilter(memberService, auditService);
        // Clear any ThreadLocal state from previous tests
        RbacAuthorizationFilter.clearThreadLocals();
    }

    @Test
    void getCurrentActor_returnsNullWhenNotSet() {
        assertNull(RbacAuthorizationFilter.getCurrentActor());
    }

    @Test
    void getCurrentActorOrDefault_returnsSystemWhenNotSet() {
        assertEquals("system", RbacAuthorizationFilter.getCurrentActorOrDefault());
    }

    @Test
    void isWriteAllowed_returnsFalseWhenNotSet() {
        assertFalse(RbacAuthorizationFilter.isWriteAllowed());
    }

    @Test
    void resolveWriteAllowed_nodeAuth_returnsTrue() {
        // Simulate node auth by setting the ThreadLocal
        RbacAuthorizationFilter.setNodeAuthForTest(true);
        boolean result = invokeResolveWriteAllowed(filter, null, true);
        assertTrue(result);
        RbacAuthorizationFilter.clearThreadLocals();
    }

    @Test
    void resolveWriteAllowed_humanWithWriteSurface_returnsTrue() {
        Human human = new Human();
        human.setRbac("admin");
        when(memberService.findHuman("h1")).thenReturn(Optional.of(human));
        when(memberService.hasWriteSurface(human)).thenReturn(true);

        boolean result = invokeResolveWriteAllowed(filter, "h1", false);

        assertTrue(result);
    }

    @Test
    void resolveWriteAllowed_humanWithoutWriteSurface_returnsFalse() {
        Human human = new Human();
        human.setRbac("viewer");
        when(memberService.findHuman("h2")).thenReturn(Optional.of(human));
        when(memberService.hasWriteSurface(human)).thenReturn(false);

        boolean result = invokeResolveWriteAllowed(filter, "h2", false);

        assertFalse(result);
    }

    @Test
    void resolveWriteAllowed_agentWithWriteSurface_returnsTrue() {
        Agent agent = new Agent();
        agent.setId("a1");
        when(memberService.findAgent("a1")).thenReturn(Optional.of(agent));
        when(memberService.hasWriteSurfaceAgent(agent)).thenReturn(true);

        boolean result = invokeResolveWriteAllowed(filter, "a1", false);

        assertTrue(result);
    }

    @Test
    void resolveWriteAllowed_unknownActor_returnsFalse() {
        when(memberService.findHuman("unknown")).thenReturn(Optional.empty());
        when(memberService.findAgent("unknown")).thenReturn(Optional.empty());

        boolean result = invokeResolveWriteAllowed(filter, "unknown", false);

        assertFalse(result);
    }

    @Test
    void resolveWriteAllowed_nullActor_returnsFalse() {
        boolean result = invokeResolveWriteAllowed(filter, null, false);
        assertFalse(result);
    }

    /**
     * Reflection helper to invoke the private resolveWriteAllowed method.
     */
    private boolean invokeResolveWriteAllowed(RbacAuthorizationFilter f, String actor, boolean nodeAuth) {
        try {
            java.lang.reflect.Method method = RbacAuthorizationFilter.class
                    .getDeclaredMethod("resolveWriteAllowed", String.class, boolean.class);
            method.setAccessible(true);
            return (boolean) method.invoke(f, actor, nodeAuth);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
