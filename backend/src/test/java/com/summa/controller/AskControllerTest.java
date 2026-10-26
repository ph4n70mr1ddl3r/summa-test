package com.summa.controller;

import com.summa.model.Ask;
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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DAT-120 on the ask surface: `asks.from` / `asks.to` carry keyed unions, and
 * the kind is resolved by live lookup — an agent addressee must keep its `a:`
 * kind, or the ask resolves against the wrong live table and its accept never
 * counts toward a quorum.
 */
class AskControllerTest {

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
    void createAsk_agentAddressee_keepsAgentKind() {
        TestAuthContext.allowWrites("human-1");
        when(memberService.toKeyedMemberRef("human-1")).thenReturn("h:human-1");
        when(memberService.toKeyedMemberRef("agent-1")).thenReturn("a:agent-1");
        when(askService.create(anyString(), anyString(), anyString(), any(), any(), any(),
                any(), any(), any(), any())).thenReturn(new Ask());

        var res = controller.createAsk(Map.of(
                "kind", "question",
                "to", "agent-1",
                "payload", "{}"));

        assertEquals(200, res.getStatusCodeValue());
        verify(askService).create(eq("question"), eq("h:human-1"), eq("a:agent-1"), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void createAsk_humanAddressee_usesHumanKind() {
        TestAuthContext.allowWrites("human-1");
        when(memberService.toKeyedMemberRef("human-1")).thenReturn("h:human-1");
        when(memberService.toKeyedMemberRef("human-2")).thenReturn("h:human-2");
        when(askService.create(anyString(), anyString(), anyString(), any(), any(), any(),
                any(), any(), any(), any())).thenReturn(new Ask());

        var res = controller.createAsk(Map.of(
                "kind", "question",
                "to", "human-2",
                "payload", "{}"));

        assertEquals(200, res.getStatusCodeValue());
        verify(askService).create(eq("question"), eq("h:human-1"), eq("h:human-2"), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void createAsk_unknownAddressee_refused() {
        TestAuthContext.allowWrites("human-1");
        when(memberService.toKeyedMemberRef("human-1")).thenReturn("h:human-1");
        when(memberService.toKeyedMemberRef("ghost")).thenReturn(null);

        var res = controller.createAsk(Map.of(
                "kind", "question",
                "to", "ghost",
                "payload", "{}"));

        assertEquals(422, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("to does not reference"));
        verify(askService, never()).create(anyString(), anyString(), anyString(), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void createAsk_adminBroadcast_allowed() {
        TestAuthContext.allowWrites("human-1");
        when(memberService.toKeyedMemberRef("human-1")).thenReturn("h:human-1");
        when(askService.create(anyString(), anyString(), anyString(), any(), any(), any(),
                any(), any(), any(), any())).thenReturn(new Ask());

        var res = controller.createAsk(Map.of(
                "kind", "question",
                "to", "admins",
                "payload", "{}"));

        assertEquals(200, res.getStatusCodeValue());
        verify(askService).create(eq("question"), eq("h:human-1"), eq("admins"), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void createAsk_withAnUnknownInitiative_isAValidationRefusal() {
        // API-061: validation before constraint — asks.initiative_id is
        // foreign-key-pinned, so an unknown one is a 422, not an integrity abort.
        TestAuthContext.allowWrites("member-1");
        when(memberService.toKeyedMemberRef("member-1")).thenReturn("h:member-1");
        when(memberService.toKeyedMemberRef("owner-1")).thenReturn("h:owner-1");
        doThrow(new IllegalArgumentException("initiativeId does not reference an existing initiative: ghost"))
            .when(askService).requireLiveReferences("ghost", null);

        var res = controller.createAsk(Map.of(
                "kind", "question",
                "to", "owner-1",
                "initiativeId", "ghost"));

        assertEquals(422, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("initiativeId"));
        verify(askService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createAsk_withAnUnknownExpiryBehavior_isAValidationRefusal() {
        // ASK-011/API-061: expiry semantics are a closed vocabulary; an unknown
        // value is refused at the door rather than surviving to the CHECK abort.
        TestAuthContext.allowWrites("member-1");
        when(memberService.toKeyedMemberRef("member-1")).thenReturn("h:member-1");
        when(memberService.toKeyedMemberRef("owner-1")).thenReturn("h:owner-1");

        var res = controller.createAsk(Map.of(
                "kind", "question",
                "to", "owner-1",
                "expiryBehavior", "maybe"));

        assertEquals(422, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("Invalid expiryBehavior"));
        verify(askService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createAsk_withAValidExpiryBehavior_passesTheStoredValue() {
        TestAuthContext.allowWrites("member-1");
        when(memberService.toKeyedMemberRef("member-1")).thenReturn("h:member-1");
        when(memberService.toKeyedMemberRef("owner-1")).thenReturn("h:owner-1");
        when(askService.create(anyString(), anyString(), anyString(), any(), any(), any(),
                any(), any(), any(), any())).thenReturn(new Ask());

        var res = controller.createAsk(Map.of(
                "kind", "question",
                "to", "owner-1",
                "expiryBehavior", "escalate"));

        assertEquals(200, res.getStatusCodeValue());
        verify(askService).create(eq("question"), eq("h:member-1"), eq("h:owner-1"), any(), any(),
                eq("escalate"), any(), any(), any(), any());
    }

    @Test
    void respond_withAnUnknownDecision_isAValidationRefusal() {
        // ASK-050/API-061: the decision vocabulary is validated at the door.
        TestAuthContext.allowWrites("member-1");
        when(memberService.toKeyedMemberRef("member-1")).thenReturn("h:member-1");
        when(askService.respond(eq("ask-1"), anyString(), anyString(), anyString()))
            .thenThrow(new IllegalArgumentException("Invalid decision: perhaps. Must be one of: accept, deny"));

        var res = controller.respond("ask-1", Map.of("response", "sure", "decision", "perhaps"));

        assertEquals(422, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("Invalid decision"));
    }

    @Test
    void respond_withADeny_passesTheDecisionToTheService() {
        TestAuthContext.allowWrites("member-1");
        when(memberService.toKeyedMemberRef("member-1")).thenReturn("h:member-1");
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("denied");
        when(askService.respond("ask-1", "h:member-1", "no thanks", "deny")).thenReturn(ask);

        var res = controller.respond("ask-1", Map.of("response", "no thanks", "decision", "deny"));

        assertEquals(200, res.getStatusCodeValue());
        verify(askService).respond("ask-1", "h:member-1", "no thanks", "deny");
    }
}
