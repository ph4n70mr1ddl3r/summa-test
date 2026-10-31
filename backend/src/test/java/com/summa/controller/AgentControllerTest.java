package com.summa.controller;

import com.summa.model.Ask;
import com.summa.model.Agent;
import com.summa.model.RoleTemplate;
import com.summa.model.AuditEvent;
import com.summa.repository.RoleTemplateRepository;
import com.summa.security.TestAuthContext;
import com.summa.security.WriteGate;
import com.summa.service.AgentService;
import com.summa.service.AuditService;
import com.summa.service.AskService;
import com.summa.service.MemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TPL-046: the pending-ask dedup guard on the promote door must query and
 * store the same keyed-union form. Promotion asks are stored with
 * from = "h:<owner>", so the guard must also query "h:<owner>" — querying
 * the bare id silently misses every row it created and lets a second
 * promotion ask through the dedup.
 */
class AgentControllerTest {

    private AgentService agentService;
    private AuditService auditService;
    private AskService askService;
    private RoleTemplateRepository roleTemplateRepository;
    private MemberService memberService;
    private AgentController controller;

    @BeforeEach
    void setUp() {
        agentService = mock(AgentService.class);
        auditService = mock(AuditService.class);
        askService = mock(AskService.class);
        roleTemplateRepository = mock(RoleTemplateRepository.class);
        memberService = mock(MemberService.class);

        controller = new AgentController(
                agentService,
                auditService,
                new WriteGate(auditService),
                askService,
                roleTemplateRepository,
                new ObjectMapper(),
                memberService);

        when(auditService.log(any(), any(), any(), any(), any())).thenAnswer(inv -> {
            AuditEvent e = new AuditEvent();
            e.setId("audit-1");
            return e;
        });
        when(auditService.logSystem(anyString(), anyString(), any(), any())).thenAnswer(inv -> {
            AuditEvent e = new AuditEvent();
            e.setId("audit-2");
            return e;
        });
    }

    @AfterEach
    void tearDown() {
        TestAuthContext.clear();
    }

    private Agent makeAgent(String id, String ownerId) {
        Agent agent = new Agent();
        agent.setId(id);
        agent.setOwnerHumanId(ownerId);
        agent.setName("Agent " + id);
        agent.setAgentClass("persistent");
        return agent;
    }

    @Test
    void promote_existingPendingAsk_refusesDuplicateWithKeyedQuery() {
        // Regression: the guard must query "h:<owner>" to match the stored
        // from column. A bare-id query silently returns empty and the dedup
        // guard is defeated — a second promotion ask is filed.
        TestAuthContext.allowWrites("owner-1");
        when(agentService.findById("agent-1")).thenReturn(Optional.of(makeAgent("agent-1", "owner-1")));
        when(memberService.isAdmin("owner-1")).thenReturn(false);

        Ask existingAsk = new Ask();
        existingAsk.setId("ask-1");
        existingAsk.setKind("promotion");
        existingAsk.setFrom("h:owner-1");
        existingAsk.setStatus("pending");
        existingAsk.setPayload("{\"agentId\":\"agent-1\"}");
        when(askService.findByFromAndStatusPending("h:owner-1")).thenReturn(List.of(existingAsk));

        var res = controller.promote("agent-1", Map.of("placement", "new:role"));

        assertEquals(403, res.getStatusCodeValue());
        // The guard must have queried the keyed form
        verify(askService).findByFromAndStatusPending("h:owner-1");
        // No new ask should have been filed
        verify(askService, never()).create(anyString(), anyString(), anyString(),
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void promote_noPendingAskFilesAskWithKeyedFrom() {
        TestAuthContext.allowWrites("owner-1");
        when(agentService.findById("agent-1")).thenReturn(Optional.of(makeAgent("agent-1", "owner-1")));
        when(memberService.isAdmin("owner-1")).thenReturn(false);
        when(askService.findByFromAndStatusPending("h:owner-1")).thenReturn(List.of());
        when(askService.create(anyString(), anyString(), anyString(), any(), any(),
                any(), any(), any(), any(), any())).thenReturn(new Ask());

        var res = controller.promote("agent-1", Map.of("placement", "new:role"));

        assertEquals(200, res.getStatusCodeValue());
        // The stored from must be the keyed form
        verify(askService).create(eq("promotion"), eq("h:owner-1"),
                eq(com.summa.service.OffboardingWalkService.ADMIN_BROADCAST),
                anyString(), eq("standard"), eq("deny"), eq(1), any(), any(), any());
    }

    @Test
    void promote_adminActorStillGetsKeyedFrom() {
        // Even an admin actor (not the owner) is stored with the h: prefix —
        // the actor id is always stripped and keyed, never stored bare.
        TestAuthContext.allowWrites("admin-1");
        when(agentService.findById("agent-1")).thenReturn(Optional.of(makeAgent("agent-1", "owner-1")));
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        when(askService.findByFromAndStatusPending("h:admin-1")).thenReturn(List.of());
        when(askService.create(anyString(), anyString(), anyString(), any(), any(),
                any(), any(), any(), any(), any())).thenReturn(new Ask());

        var res = controller.promote("agent-1", Map.of("placement", "new:role"));

        assertEquals(200, res.getStatusCodeValue());
        verify(askService).create(eq("promotion"), eq("h:admin-1"),
                eq(com.summa.service.OffboardingWalkService.ADMIN_BROADCAST),
                anyString(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void promote_bareActorIdInGuardQueryIsTheBug() {
        // This test documents the exact regression scenario: if the guard
        // queried "owner-1" (bare) instead of "h:owner-1" (keyed), the
        // repository would return empty and the dedup would be defeated.
        // We verify the guard queries the keyed form.
        TestAuthContext.allowWrites("owner-1");
        when(agentService.findById("agent-1")).thenReturn(Optional.of(makeAgent("agent-1", "owner-1")));
        when(memberService.isAdmin("owner-1")).thenReturn(false);

        Ask existingAsk = new Ask();
        existingAsk.setId("ask-1");
        existingAsk.setKind("promotion");
        existingAsk.setFrom("h:owner-1");
        existingAsk.setStatus("pending");
        existingAsk.setPayload("{\"agentId\":\"agent-1\"}");

        // The keyed form must be what's queried — not the bare form
        when(askService.findByFromAndStatusPending("h:owner-1")).thenReturn(List.of(existingAsk));
        // The bare form would return empty (simulating the bug)
        when(askService.findByFromAndStatusPending("owner-1")).thenReturn(List.of());

        var res = controller.promote("agent-1", Map.of("placement", "new:role"));

        assertEquals(403, res.getStatusCodeValue());
        verify(askService).findByFromAndStatusPending("h:owner-1");
        verify(askService, never()).findByFromAndStatusPending("owner-1");
        verify(askService, never()).create(anyString(), anyString(), anyString(),
                any(), any(), any(), any(), any(), any(), any());
    }
}
