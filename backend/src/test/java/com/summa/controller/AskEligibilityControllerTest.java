package com.summa.controller;

import com.summa.model.AuditEvent;
import com.summa.security.RateLimiter;
import com.summa.security.TestAuthContext;
import com.summa.security.WriteGate;
import com.summa.service.AskService;
import com.summa.service.AuditService;
import com.summa.service.MemberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ORG-020's ask-target rule and DAT-080/ASK-055's admin broadcast on the ask
 * surface:
 *
 * <ul>
 *   <li>a viewer is never an ask target and a non-active member is refused
 *       every answerable post at write — the member filing door enforces it;</li>
 *   <li>the reserved `admins` broadcast renders against the live admin set —
 *       "every active admin, evaluated at render and respond time" — so an
 *       admin's inbox carries the broadcast it addresses;</li>
 *   <li>the reserved system originator is never a target.</li>
 * </ul>
 */
class AskEligibilityControllerTest {

    private AskService askService;
    private AuditService auditService;
    private MemberService memberService;
    private RateLimiter rateLimiter;
    private AskController controller;

    @BeforeEach
    void setUp() {
        askService = mock(AskService.class);
        auditService = mock(AuditService.class);
        memberService = mock(MemberService.class);
        rateLimiter = mock(RateLimiter.class);
        controller = new AskController(askService, auditService, new WriteGate(auditService),
                memberService, rateLimiter);
        when(auditService.log(any(), any(), any(), any(), any())).thenReturn(newAuditEvent());
        when(auditService.logSystem(anyString(), anyString(), any(), any())).thenReturn(newAuditEvent());
        when(rateLimiter.allow(anyString())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        TestAuthContext.clear();
    }

    private AuditEvent newAuditEvent() {
        AuditEvent event = new AuditEvent();
        event.setId("audit-1");
        return event;
    }

    private String bodyMessage(org.springframework.http.ResponseEntity<?> res) {
        Object body = res.getBody();
        if (body instanceof Map<?, ?> map) {
            Object message = map.get("message");
            return message == null ? null : message.toString();
        }
        return null;
    }

    @Test
    void createAsk_refusesAViewerAddressee() {
        // ORG-020: a viewer is never an ask target.
        TestAuthContext.allowWrites("human-1");
        when(memberService.toKeyedMemberRef("human-1")).thenReturn("h:human-1");
        when(memberService.toKeyedMemberRef("viewer-1")).thenReturn("h:viewer-1");
        doThrow(new IllegalStateException("Viewers cannot be ask targets: h:viewer-1"))
                .when(memberService).requireEligibleAskTarget("h:viewer-1");

        var res = controller.createAsk(Map.of(
                "kind", "question",
                "to", "viewer-1",
                "payload", "{}"));

        assertEquals(403, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("Viewers cannot be ask targets"));
        verify(askService, never()).create(anyString(), anyString(), anyString(), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void createAsk_refusesANonActiveAddressee() {
        // ORG-020: a non-active member is refused every answerable post at write.
        TestAuthContext.allowWrites("human-1");
        when(memberService.toKeyedMemberRef("human-1")).thenReturn("h:human-1");
        when(memberService.toKeyedMemberRef("gone-1")).thenReturn("h:gone-1");
        doThrow(new IllegalStateException("Non-active members cannot be ask targets: h:gone-1"))
                .when(memberService).requireEligibleAskTarget("h:gone-1");

        var res = controller.createAsk(Map.of(
                "kind", "question",
                "to", "gone-1",
                "payload", "{}"));

        assertEquals(403, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("Non-active members cannot be ask targets"));
    }

    @Test
    void createAsk_refusesTheSystemOriginatorAsTarget() {
        // ASK-031: the system originator is never a target, and `asks.to`'s CHECK
        // admits only `admins` or a keyed union.
        TestAuthContext.allowWrites("human-1");
        when(memberService.toKeyedMemberRef("human-1")).thenReturn("h:human-1");

        var res = controller.createAsk(Map.of(
                "kind", "question",
                "to", "system",
                "payload", "{}"));

        assertEquals(422, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("system originator"));
    }

    @Test
    void listAsks_adminSeesTheAdminBroadcast() {
        // DAT-080/ASK-055: the broadcast renders against the live admin set — an
        // admin's inbox carries the `admins`-addressed asks.
        TestAuthContext.allowWrites("admin-1");
        when(memberService.toKeyedMemberRef("admin-1")).thenReturn("h:admin-1");
        when(memberService.findHuman("admin-1")).thenReturn(java.util.Optional.of(adminHuman()));
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        when(askService.findByTo("h:admin-1", 50)).thenReturn(List.of());
        when(askService.findByTo("admins", 50)).thenReturn(List.of(newAsk("broadcast-1")));

        var res = controller.listAsks(null, null, 50);

        assertEquals(200, res.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        List<?> body = (List<?>) res.getBody();
        assertEquals(1, body.size());
    }

    @Test
    void listAsks_nonAdminDoesNotSeeTheAdminBroadcast() {
        TestAuthContext.allowWrites("member-1");
        when(memberService.toKeyedMemberRef("member-1")).thenReturn("h:member-1");
        when(memberService.findHuman("member-1")).thenReturn(java.util.Optional.of(memberHuman()));
        when(memberService.isAdmin("member-1")).thenReturn(false);
        when(askService.findByTo("h:member-1", 50)).thenReturn(List.of());

        var res = controller.listAsks(null, null, 50);

        assertEquals(200, res.getStatusCodeValue());
        verify(askService, never()).findByTo("admins", 50);
    }

    private com.summa.model.Human adminHuman() {
        com.summa.model.Human human = new com.summa.model.Human();
        human.setId("admin-1");
        human.setRbac("admin");
        return human;
    }

    private com.summa.model.Human memberHuman() {
        com.summa.model.Human human = new com.summa.model.Human();
        human.setId("member-1");
        human.setRbac("member");
        return human;
    }

    private com.summa.model.Ask newAsk(String id) {
        com.summa.model.Ask ask = new com.summa.model.Ask();
        ask.setId(id);
        ask.setKind("question");
        ask.setFrom("system");
        ask.setTo("admins");
        ask.setStatus("pending");
        ask.setDeadline(java.time.Instant.now().plusSeconds(3600));
        return ask;
    }
}
