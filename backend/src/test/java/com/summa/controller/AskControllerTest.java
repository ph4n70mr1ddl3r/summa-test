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
}
