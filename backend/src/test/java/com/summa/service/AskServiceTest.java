package com.summa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.summa.repository.AskRepository;
import com.summa.repository.InitiativeRepository;
import com.summa.model.Ask;
import com.summa.model.Human;
import com.summa.model.Initiative;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AskServiceTest {

    @Mock
    private AskRepository askRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private MemberService memberService;

    @Mock
    private GovernanceService governanceService;

    @Mock
    private InitiativeRepository initiativeRepository;

    @Mock
    private InitiativeService initiativeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AskService buildService() {
        return new AskService(askRepository, auditService, memberService, governanceService, initiativeRepository, initiativeService, 1L, objectMapper);
    }

    @Test
    void createAsk_withDefaultValues() {
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setKind("approval");
        ask.setFrom("agent-1");
        ask.setTo("human-1");
        ask.setSlaTier("standard");
        ask.setExpiryBehavior("deny");

        when(askRepository.save(any())).thenReturn(ask);

        AskService svc = buildService();
        Ask result = svc.create(
            "approval", "agent-1", "human-1", "{}",
            "standard", "deny", 1,
            Instant.now().plusSeconds(3600), null, null
        );

        assertNotNull(result);
        assertEquals("approval", result.getKind());
    }

    @Test
    void createAsk_refusesPastDeadline() {
        AskService svc = buildService();
        assertThrows(IllegalArgumentException.class, () -> {
            svc.create(
                "approval", "agent-1", "human-1", "{}",
                "standard", "deny", 1,
                Instant.now().minusSeconds(3600), null, null
            );
        });
    }

    @Test
    void respond_updatesStatus() {
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setTo("human-1");
        ask.setQuorumRequired(1);

        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        when(askRepository.save(any())).thenReturn(ask);

        AskService svc = buildService();
        Ask result = svc.respond("ask-1", "human-1", "approved");

        assertEquals("answered", result.getStatus());
        assertNotNull(result.getRespondedAt());
    }

    @Test
    void expire_updatesStatus() {
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setDeadline(Instant.now().minusSeconds(3600));

        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        when(askRepository.save(any())).thenReturn(ask);

        AskService svc = buildService();
        Ask result = svc.expire("ask-1");

        assertEquals("expired", result.getStatus());
    }

    @Test
    void respond_quorumN_GreaterThan1_requiresPoolPrincipal() {
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setTo("human-1");
        ask.setQuorumRequired(2);
        ask.setResponses("[]");

        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        when(askRepository.save(any())).thenReturn(ask);

        Human human1 = new Human();
        human1.setId("human-1");
        human1.setRbac("member");
        lenient().when(memberService.findHuman("human-1")).thenReturn(Optional.of(human1));

        AskService svc = buildService();
        Ask result = svc.respond("ask-1", "human-1", "accept");

        // First principal response counts toward quorum
        assertEquals("pending", result.getStatus());
    }

    @Test
    void respond_quorumN_GreaterThan1_auditOnlyForDeputy() {
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setTo("human-1");
        ask.setQuorumRequired(2);
        ask.setResponses("[]");

        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        lenient().when(askRepository.save(any())).thenReturn(ask);

        Human human1 = new Human();
        human1.setId("human-1");
        human1.setRbac("member");
        human1.setDeputyMemberId("human-2");
        lenient().when(memberService.findHuman("human-1")).thenReturn(Optional.of(human1));

        AskService svc = buildService();
        // Deputy responds — should be audit-only, not count toward quorum
        Ask result = svc.respond("ask-1", "human-2", "accept");

        // Status remains pending since deputy's accept is audit-only for N>1
        assertEquals("pending", result.getStatus());
        verify(auditService).logSystem(eq("AUDIT_ONLY_QUORUM_RESPONSE"), eq("ask"), eq("ask-1"), anyString());
    }

    @Test
    void respond_systemOriginatedAsk_allowsHumanResponder() {
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setFrom("system");
        ask.setTo("human-1");
        ask.setQuorumRequired(1);

        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        when(askRepository.save(any())).thenReturn(ask);

        AskService svc = buildService();
        Ask result = svc.respond("ask-1", "human-1", "approved");

        assertEquals("answered", result.getStatus());
    }

    @Test
    void respond_systemOriginatedAsk_rejectsSystemResponder() {
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setFrom("human-1");
        ask.setTo("system");
        ask.setQuorumRequired(1);

        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));

        AskService svc = buildService();
        assertThrows(IllegalArgumentException.class, () -> {
            svc.respond("ask-1", "system", "approved");
        });
    }

    @Test
    void processExpiredAsks_escalate_createsSuccessor() {
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setExpiryBehavior("escalate");
        ask.setSlaTier("standard");
        ask.setDeadline(Instant.now().minusSeconds(3600));
        ask.setFrom("agent-1");
        ask.setTo("human-1");
        ask.setQuorumRequired(1);
        ask.setKind("approval");

        when(askRepository.findExpiredBefore(any())).thenReturn(List.of(ask));
        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        when(askRepository.save(any())).thenReturn(ask);
        lenient().when(askRepository.findByToAndStatusPending(any())).thenReturn(List.of());

        Human human1 = new Human();
        human1.setId("human-1");
        human1.setDeputyMemberId("human-2");
        lenient().when(memberService.findHuman("human-1")).thenReturn(Optional.of(human1));
        when(memberService.resolveDeputyTarget("human-1")).thenReturn(Optional.of("human-2"));

        Ask successor = new Ask();
        successor.setId("ask-2");
        successor.setTo("human-2");
        lenient().when(askRepository.save(any())).thenReturn(successor);

        AskService svc = buildService();
        svc.processExpiredAsks();

        assertEquals("expired", ask.getStatus());
        verify(askRepository, atLeastOnce()).save(any());
    }

    @Test
    void handleDirectionAskResponse_extend_noOpOnGoalRef() {
        Ask ask = new Ask();
        ask.setId("ask-dir-1");
        ask.setKind("question");
        ask.setSlaTier("bulk");
        ask.setInitiativeId("init-1");
        ask.setPayload("{\"goalId\":\"goal-1\"}");

        InitiativeService initSvc = mock(InitiativeService.class);
        AskService svc = new AskService(askRepository, auditService, memberService, governanceService,
            initiativeRepository, initSvc, 1L, objectMapper);

        svc.handleDirectionAskResponse(ask, "extend");

        verify(initiativeService, never()).close(anyString(), anyString());
        verify(auditService, atLeastOnce()).logSystem(eq("DIRECTION_EXTEND"), eq("ask"), eq("ask-dir-1"), anyString());
    }

    @Test
    void handleDirectionAskResponse_close_callsInitiativeClose() {
        Ask ask = new Ask();
        ask.setId("ask-dir-2");
        ask.setKind("question");
        ask.setSlaTier("bulk");
        ask.setInitiativeId("init-2");
        ask.setPayload("{}");

        Initiative closeInit = new Initiative();
        closeInit.setId("init-2");
        lenient().when(initiativeRepository.findById("init-2")).thenReturn(Optional.of(closeInit));

        AskService svc = buildService();

        svc.handleDirectionAskResponse(ask, "close");

        verify(initiativeService).close("init-2", "system");
        verify(auditService, atLeastOnce()).logSystem(eq("DIRECTION_CLOSE"), eq("ask"), eq("ask-dir-2"), anyString());
    }

    @Test
    void handleDirectionAskResponse_rebase_updatesGoalRef() {
        Ask ask = new Ask();
        ask.setId("ask-dir-3");
        ask.setKind("question");
        ask.setSlaTier("bulk");
        ask.setInitiativeId("init-3");
        ask.setPayload("{\"newGoalRef\":\"goal-new-1\"}");

        Initiative init = new Initiative();
        init.setId("init-3");
        when(initiativeRepository.findById("init-3")).thenReturn(Optional.of(init));

        AskService svc = buildService();

        svc.handleDirectionAskResponse(ask, "re-base");

        assertEquals("goal-new-1", init.getGoalRef());
        verify(initiativeRepository).save(init);
        verify(auditService, atLeastOnce()).logSystem(eq("DIRECTION_REBASE"), eq("ask"), eq("init-3"), anyString());
    }

    @Test
    void handleDirectionAskResponse_noInitiativeId_returnsEarly() {
        Ask ask = new Ask();
        ask.setId("ask-dir-4");
        ask.setKind("question");
        ask.setSlaTier("bulk");
        ask.setInitiativeId(null);
        ask.setPayload("{}");

        AskService svc = buildService();

        svc.handleDirectionAskResponse(ask, "re-base");

        verify(initiativeRepository, never()).findById(anyString());
        verify(auditService, never()).logSystem(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void handleDirectionAskResponse_wrongKind_returnsEarly() {
        Ask ask = new Ask();
        ask.setId("ask-dir-5");
        ask.setKind("approval");
        ask.setSlaTier("bulk");
        ask.setInitiativeId("init-5");
        ask.setPayload("{}");

        AskService svc = buildService();

        svc.handleDirectionAskResponse(ask, "re-base");

        verify(initiativeRepository, never()).findById(anyString());
    }

    @Test
    void handleDirectionAskResponse_nonDirectionTier_returnsEarly() {
        Ask ask = new Ask();
        ask.setId("ask-dir-6");
        ask.setKind("question");
        ask.setSlaTier("standard");
        ask.setInitiativeId("init-6");
        ask.setPayload("{}");

        AskService svc = buildService();

        svc.handleDirectionAskResponse(ask, "re-base");

        verify(initiativeRepository, never()).findById(anyString());
    }

    @Test
    void processExpiredAsks_deny_expiresAsk() {
        Ask ask = new Ask();
        ask.setId("ask-expire-1");
        ask.setStatus("pending");
        ask.setExpiryBehavior("deny");
        ask.setSlaTier("standard");
        ask.setDeadline(Instant.now().minusSeconds(3600));

        when(askRepository.findExpiredBefore(any())).thenReturn(List.of(ask));
        when(askRepository.findById("ask-expire-1")).thenReturn(Optional.of(ask));
        when(askRepository.save(any())).thenReturn(ask);

        AskService svc = buildService();
        svc.processExpiredAsks();

        assertEquals("expired", ask.getStatus());
        verify(auditService).logSystem(eq("EXPIRE"), eq("ask"), eq("ask-expire-1"), isNull());
    }

    @Test
    void processExpiredAsks_skipsAlreadyNonPending() {
        Ask ask = new Ask();
        ask.setId("ask-already-done");
        ask.setStatus("answered");
        ask.setDeadline(Instant.now().minusSeconds(3600));

        when(askRepository.findExpiredBefore(any())).thenReturn(List.of(ask));
        Ask fresh = new Ask();
        fresh.setId("ask-already-done");
        fresh.setStatus("answered");
        when(askRepository.findById("ask-already-done")).thenReturn(Optional.of(fresh));

        AskService svc = buildService();
        svc.processExpiredAsks();

        verify(askRepository, never()).save(any());
    }

    @Test
    void toJsonResponseList_preservesPriorResponses() throws Exception {
        AskService svc = buildService();
        // Use reflection to invoke the private method
        java.lang.reflect.Method method = AskService.class.getDeclaredMethod(
            "toJsonResponseList", List.class, String.class, String.class);
        method.setAccessible(true);

        String existing = "[{\"responder\":\"h1\",\"response\":\"answer1\"}]";
        List<String> ids = List.of("h1", "h2");
        String result = (String) method.invoke(svc, ids, "answer2", existing);

        List<Map<String, String>> entries = objectMapper.readValue(result,
            new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {});
        assertEquals(2, entries.size());
        assertEquals("answer1", entries.get(0).get("response"));
        assertEquals("answer2", entries.get(1).get("response"));
    }

    @Test
    void toJsonResponseList_doesNotSpreadLatestToPriorResponders() throws Exception {
        AskService svc = buildService();
        java.lang.reflect.Method method = AskService.class.getDeclaredMethod(
            "toJsonResponseList", List.class, String.class, String.class);
        method.setAccessible(true);

        String existing = "[{\"responder\":\"h1\",\"response\":\"old-answer\"}]";
        List<String> ids = List.of("h1", "h2", "h3");
        String result = (String) method.invoke(svc, ids, "new-answer", existing);

        List<Map<String, String>> entries = objectMapper.readValue(result,
            new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {});
        assertEquals(3, entries.size());
        assertEquals("old-answer", entries.get(0).get("response"));
        // Only the last missing responder (h3) gets latestResponse; h2 gets empty
        assertEquals("", entries.get(1).get("response"));
        assertEquals("new-answer", entries.get(2).get("response"));
    }

    @Test
    void findByToAndStatus_queriesTargetedStatus() {
        Ask ask = new Ask();
        ask.setId("ask-9");
        when(askRepository.findByToAndStatusOrdered("h:user-1", "answered", 25))
            .thenReturn(List.of(ask));

        AskService svc = buildService();
        List<Ask> result = svc.findByToAndStatus("h:user-1", "answered", 25);

        assertEquals(1, result.size());
        assertEquals("ask-9", result.get(0).getId());
        verify(askRepository).findByToAndStatusOrdered("h:user-1", "answered", 25);
    }

    @Test
    void create_rearmsStormCollapseWindowAfterNonCollapsingCreate() {
        // ASK-100: a create that does not collapse must (re)arm the window for its
        // key, so a following duplicate within the window still collapses. Before
        // the fix, an expired slot was never refreshed and duplicates passed forever.
        AskService svc = buildService();
        when(askRepository.save(any(Ask.class))).thenAnswer(i -> {
            Ask a = (Ask) i.getArgument(0);
            if (a.getId() == null) a.setId("ask-new");
            return a;
        });

        Instant deadline = Instant.now().plusSeconds(600);
        when(askRepository.findByToAndStatusPending("h:user-2")).thenReturn(List.of());
        Ask second = svc.create("question", "h:user-1", "h:user-2", "other-payload", "standard", "deny", 1, deadline, null, null);
        assertNotNull(second.getId());

        // A duplicate of that ask within the live window must now collapse:
        Ask canonical = new Ask();
        canonical.setId("ask-canonical");
        canonical.setTo("h:user-2");
        canonical.setPayload("other-payload");
        canonical.setKind("question");
        when(askRepository.findByToAndStatusPending("h:user-2")).thenReturn(List.of(canonical));
        Ask collapsed = svc.create("question", "h:user-1", "h:user-2", "other-payload", "standard", "deny", 1, deadline, null, null);
        assertEquals("ask-canonical", collapsed.getId());
        assertEquals(1, canonical.getCollapsedCount());
    }

    @Test
    void create_defaultsTheSlaTierWhenAbsent() {
        // The tier is optional at the API door and the row defaults to
        // 'standard' — an absent tier must resolve to the default, not be
        // refused as an unknown value.
        AskService svc = buildService();
        when(askRepository.save(any(Ask.class))).thenAnswer(i -> {
            Ask a = (Ask) i.getArgument(0);
            if (a.getId() == null) a.setId("ask-new");
            return a;
        });
        when(askRepository.findByToAndStatusPending("h:user-2")).thenReturn(List.of());

        Instant deadline = Instant.now().plusSeconds(600);
        Ask ask = svc.create("question", "h:user-1", "h:user-2", "{}", null, null, null, deadline, null, null);

        assertEquals("standard", ask.getSlaTier());
        assertEquals("deny", ask.getExpiryBehavior());
        assertEquals(1, ask.getQuorumRequired());
    }

    @Test
    void create_refusesAnUnknownSlaTier() {
        AskService svc = buildService();
        Instant deadline = Instant.now().plusSeconds(600);

        assertThrows(IllegalArgumentException.class, () ->
            svc.create("question", "h:user-1", "h:user-2", "{}", "urgent", "deny", 1, deadline, null, null));
    }
}
