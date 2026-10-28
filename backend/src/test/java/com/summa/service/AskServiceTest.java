package com.summa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.summa.repository.AskRepository;
import com.summa.repository.InitiativeRepository;
import com.summa.repository.WorkspaceRepository;
import com.summa.repository.DnaDomainRepository;
import com.summa.model.Ask;
import com.summa.model.Human;
import com.summa.model.Initiative;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private DnaDomainRepository domainRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AskService buildService() {
        return new AskService(askRepository, auditService, memberService, governanceService, initiativeRepository, workspaceRepository, domainRepository, initiativeService, 1L, objectMapper);
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
    void respond_quorumN_GreaterThan1_activeAdminIsAPoolPrincipal() {
        // ASK-051: the eligible pool of a quorum ask is the pool's primary
        // recipient plus every active admin, evaluated at respond time. Without
        // the admin in the pool an N>1 ask addressed to a single owner could never
        // reach quorum — the owner alone cannot supply N approvals and every
        // admin was refused at the door.
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setTo("h:owner-1");
        ask.setQuorumRequired(2);
        ask.setResponses("[]");
        ask.setDeadline(Instant.now().plusSeconds(3600));

        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        when(askRepository.save(any())).thenReturn(ask);

        Human admin = new Human();
        admin.setId("admin-1");
        admin.setRbac("admin");
        lenient().when(memberService.findAdmins()).thenReturn(List.of(admin));
        lenient().when(memberService.findHuman("owner-1")).thenReturn(Optional.empty());

        AskService svc = buildService();
        Ask result = svc.respond("ask-1", "admin-1", "accept");

        // The admin's accept counts toward the quorum — one of two.
        assertEquals("pending", result.getStatus());
        assertTrue(result.getResponses().contains("admin-1"));
    }

    @Test
    void respond_quorumN_GreaterThan1_nonPoolMemberIsRefused() {
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setTo("h:owner-1");
        ask.setQuorumRequired(2);
        ask.setResponses("[]");
        ask.setDeadline(Instant.now().plusSeconds(3600));

        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        lenient().when(memberService.findAdmins()).thenReturn(List.of());
        lenient().when(memberService.findHuman("owner-1")).thenReturn(Optional.empty());

        AskService svc = buildService();
        assertThrows(IllegalArgumentException.class, () -> svc.respond("ask-1", "stranger-1", "accept"));
    }

    @Test
    void respond_afterTheDeadline_isAuditOnlyAndDoesNotBind() {
        // ASK-015/ASK-050: expiry denies — a response landing after the deadline
        // is recorded with no effect. The deadline is a hard cut, not a sweep
        // granularity: between the deadline and the next sweep tick the ask is
        // still 'pending' and a late accept must not close it 'answered'.
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setTo("h:human-1");
        ask.setQuorumRequired(1);
        ask.setResponses("[]");
        ask.setDeadline(Instant.now().minusSeconds(60));

        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));

        Human human1 = new Human();
        human1.setId("human-1");
        human1.setRbac("member");
        lenient().when(memberService.findHuman("human-1")).thenReturn(Optional.of(human1));

        AskService svc = buildService();
        Ask result = svc.respond("ask-1", "human-1", "accept");

        assertEquals("pending", result.getStatus());
        assertEquals("[]", result.getResponses());
        verify(askRepository, never()).save(any());
        verify(auditService).logSystem(eq("AUDIT_ONLY_STALE_RESPONSE"), eq("ask"), eq("ask-1"), anyString());
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
            initiativeRepository, workspaceRepository, domainRepository, initSvc, 1L, objectMapper);

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
        // ASK-053: the expiry row names the quorum, the eligible pool and the gap.
        verify(auditService).logSystem(eq("EXPIRE"), eq("ask"), eq("ask-expire-1"),
            org.mockito.ArgumentMatchers.contains("shortfall"));
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

    // --- ASK-050: the denial terminal and the decision vocabulary ---

    @Test
    void respond_withADeny_closesTheAskDenied() {
        // ASK-050: "a deny closes it denied immediately" — the terminal is not
        // 'answered', and the decision is recorded on the ledger entry.
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setTo("h:human-1");
        ask.setQuorumRequired(1);
        ask.setResponses("[]");
        ask.setDeadline(Instant.now().plusSeconds(3600));

        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        when(askRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Human human1 = new Human();
        human1.setId("human-1");
        human1.setRbac("member");
        lenient().when(memberService.findHuman("human-1")).thenReturn(Optional.of(human1));

        AskService svc = buildService();
        Ask result = svc.respond("ask-1", "human-1", "not now", "deny");

        assertEquals("denied", result.getStatus());
        assertTrue(result.getResponses().contains("\"decision\":\"deny\""));
    }

    @Test
    void respond_withADeny_onAQuorumAsk_closesItDeniedImmediately() {
        // ASK-050: a denial is terminal whatever the quorum is — it does not wait
        // for the Nth accept.
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setTo("h:human-1");
        ask.setQuorumRequired(3);
        ask.setResponses("[]");
        ask.setDeadline(Instant.now().plusSeconds(3600));

        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        when(askRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Human human1 = new Human();
        human1.setId("human-1");
        human1.setRbac("member");
        lenient().when(memberService.findHuman("human-1")).thenReturn(Optional.of(human1));
        lenient().when(memberService.findAdmins()).thenReturn(List.of());

        AskService svc = buildService();
        Ask result = svc.respond("ask-1", "human-1", "no", "deny");

        assertEquals("denied", result.getStatus());
    }

    @Test
    void respond_aDenialDoesNotCountTowardTheQuorum() {
        // ASK-050: the ask closes answered once N distinct ACCEPTS land — a
        // response that is not an accept never counts toward the quorum.
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setTo("h:human-1");
        ask.setQuorumRequired(2);
        ask.setResponses("[]");
        ask.setDeadline(Instant.now().plusSeconds(3600));

        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        when(askRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Human human1 = new Human();
        human1.setId("human-1");
        human1.setRbac("member");
        lenient().when(memberService.findHuman("human-1")).thenReturn(Optional.of(human1));
        lenient().when(memberService.findAdmins()).thenReturn(List.of());

        AskService svc = buildService();
        Ask result = svc.respond("ask-1", "human-1", "looks fine", "accept");

        assertEquals("pending", result.getStatus());
        assertTrue(result.getResponses().contains("\"decision\":\"accept\""));
    }

    @Test
    void respond_withAnUnknownDecision_isRefused() {
        // API-061: the decision vocabulary is validated at the door.
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setTo("h:human-1");
        ask.setQuorumRequired(1);
        ask.setResponses("[]");
        ask.setDeadline(Instant.now().plusSeconds(3600));

        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));

        Human human1 = new Human();
        human1.setId("human-1");
        human1.setRbac("member");
        lenient().when(memberService.findHuman("human-1")).thenReturn(Optional.of(human1));

        AskService svc = buildService();
        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
            () -> svc.respond("ask-1", "human-1", "maybe", "perhaps"));
        assertTrue(refusal.getMessage().contains("Invalid decision"));
    }

    @Test
    void respond_quorumN_anAgentAddressee_isNotAPoolPrincipal() {
        // ASK-050/052/092: toward N > 1 only human members count — a delegated
        // agent's accept is audit-only, exactly like a deputy's.
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setTo("a:agent-1");
        ask.setQuorumRequired(2);
        ask.setResponses("[]");
        ask.setDeadline(Instant.now().plusSeconds(3600));

        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        lenient().when(memberService.findHuman("agent-1")).thenReturn(Optional.empty());
        lenient().when(memberService.findAdmins()).thenReturn(List.of());

        AskService svc = buildService();
        Ask result = svc.respond("ask-1", "agent-1", "done", "accept");

        // Audit-only: the ask does not close and the ledger records nothing.
        assertEquals("pending", result.getStatus());
        assertEquals("[]", result.getResponses());
        verify(auditService).logSystem(eq("AUDIT_ONLY_QUORUM_RESPONSE"), eq("ask"), eq("ask-1"), anyString());
    }

    @Test
    void expire_namesTheQuorumShortfall() {
        // ASK-053: an unanswerable quorum ask denies at expiry, fail-safe, "with
        // the breach escalation naming the shortfall".
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setTo("h:owner-1");
        ask.setQuorumRequired(3);
        ask.setResponses("[]");
        ask.setExpiryBehavior("deny");
        ask.setDeadline(Instant.now().plusSeconds(3600));

        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        when(askRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Human owner = new Human();
        owner.setId("owner-1");
        owner.setRbac("owner");
        lenient().when(memberService.findHuman("owner-1")).thenReturn(Optional.of(owner));
        Human admin = new Human();
        admin.setId("admin-1");
        lenient().when(memberService.findAdmins()).thenReturn(List.of(admin));

        AskService svc = buildService();
        Ask result = svc.expire("ask-1");

        assertEquals("expired", result.getStatus());
        verify(auditService).logSystem(eq("EXPIRE"), eq("ask"), eq("ask-1"),
            org.mockito.ArgumentMatchers.contains("shortfall"));
    }

    @Test
    void requireLiveReferences_refusesAnUnknownInitiative() {
        // API-061: the ask's optional references are foreign-key-pinned — an
        // unknown one is a validation refusal, not an integrity abort.
        when(initiativeRepository.existsById("ghost")).thenReturn(false);
        AskService svc = buildService();
        assertThrows(IllegalArgumentException.class,
            () -> svc.requireLiveReferences("ghost", null));
    }

    @Test
    void requireLiveReferences_refusesAnUnknownWorkspace() {
        when(workspaceRepository.existsById("ghost-ws")).thenReturn(false);
        AskService svc = buildService();
        assertThrows(IllegalArgumentException.class,
            () -> svc.requireLiveReferences(null, "ghost-ws"));
    }

    @Test
    void create_flagsAQuorumLargerThanTheEligiblePool() {
        // ASK-053: N is pool-checked at every write door — an N larger than the
        // eligible approver pool flags at write like any contradiction. The ask is
        // still filed (a live pool can grow back, ASK-044), but the shortfall is
        // named so the write is never a silent impossibility.
        AskService svc = buildService();
        when(askRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Human owner = new Human();
        owner.setId("owner-1");
        owner.setRbac("owner");
        lenient().when(memberService.findHuman("owner-1")).thenReturn(Optional.of(owner));
        lenient().when(memberService.findAdmins()).thenReturn(List.of());

        svc.create("approval", "h:proposer", "h:owner-1", "{}", "standard", "deny", 3,
            Instant.now().plusSeconds(3600), null, null);

        verify(auditService).logSystem(eq("ASK_QUORUM_POOL_SHORTFALL"), eq("ask"), anyString(),
            org.mockito.ArgumentMatchers.contains("shortfall"));
    }

    @Test
    void create_doesNotFlagAQuorumThePoolCanMeet() {
        AskService svc = buildService();
        when(askRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Human owner = new Human();
        owner.setId("owner-1");
        owner.setRbac("owner");
        lenient().when(memberService.findHuman("owner-1")).thenReturn(Optional.of(owner));
        lenient().when(memberService.findAdmins()).thenReturn(List.of());

        svc.create("approval", "h:proposer", "h:owner-1", "{}", "standard", "deny", 1,
            Instant.now().plusSeconds(3600), null, null);

        verify(auditService, never()).logSystem(eq("ASK_QUORUM_POOL_SHORTFALL"), eq("ask"), anyString(), anyString());
    }

    @Test
    void respond_auditsAnIneligibleResponder() {
        // ASK-040: a response from outside the ask's eligible set is refused at
        // the respond endpoint with the attempt audited.
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setTo("h:owner-1");
        ask.setQuorumRequired(1);
        ask.setResponses("[]");
        ask.setDeadline(Instant.now().plusSeconds(3600));

        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        lenient().when(memberService.findAdmins()).thenReturn(List.of());
        lenient().when(memberService.findHuman("owner-1")).thenReturn(Optional.empty());

        AskService svc = buildService();
        assertThrows(IllegalArgumentException.class, () -> svc.respond("ask-1", "stranger-1", "accept"));
        verify(auditService).logSystem(eq("REFUSAL"), eq("ask_respond"), eq("ask-1"), anyString());
    }

    @Test
    void expireNow_refusesANonAdmin() {
        // ASK-032: the expiry closure is one of the system's named closures — a
        // member may not force-close an ask through it.
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setTo("h:owner-1");
        ask.setQuorumRequired(1);
        ask.setDeadline(Instant.now().plusSeconds(3600));

        AskService svc = buildService();
        assertThrows(IllegalStateException.class, () -> svc.expireNow("ask-1", "member-1"));
        verify(askRepository, never()).save(any());
    }

    @Test
    void expireNow_opensASuccessorForAnEscalatingAsk() {
        // ASK-011: `escalate` and `reassign` close the expired ask and open a
        // linked successor ask. The manual door honours the behavior exactly as
        // the scheduled sweep does.
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setKind("question");
        ask.setStatus("pending");
        ask.setFrom("h:proposer");
        ask.setTo("h:owner-1");
        ask.setSlaTier("standard");
        ask.setExpiryBehavior("escalate");
        ask.setQuorumRequired(1);
        ask.setPayload("{}");
        ask.setDeadline(Instant.now().plusSeconds(3600));
        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        when(askRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(memberService.isAdmin("admin-1")).thenReturn(true);
        lenient().when(governanceService.getSetting(anyString(), eq(Number.class))).thenReturn(null);

        AskService svc = buildService();
        Ask result = svc.expireNow("ask-1", "admin-1");

        assertEquals("expired", result.getStatus());
        verify(auditService).log(eq("admin-1"), eq("EXPIRE_SUCCESSOR_CREATED"), eq("ask"), eq("ask-1"), anyString());
    }

    @Test
    void expireNow_successorCarriesTheChainRootAndWalksTheDeputyHop() {
        // ASK-057/060: the manual door's successor carries the chain root so depth
        // accounting survives across both doors, and the deputy hop is walked for
        // `escalate` exactly as on the sweep — member → deputy → … → admin.
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setKind("question");
        ask.setStatus("pending");
        ask.setFrom("h:proposer");
        ask.setTo("h:owner-1");
        ask.setSlaTier("standard");
        ask.setExpiryBehavior("escalate");
        ask.setQuorumRequired(1);
        ask.setPayload("{}");
        ask.setDeadline(Instant.now().plusSeconds(3600));
        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        when(askRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(memberService.isAdmin("admin-1")).thenReturn(true);
        lenient().when(governanceService.getSetting(anyString(), eq(Number.class))).thenReturn(null);
        lenient().when(memberService.resolveDeputyTarget("h:owner-1")).thenReturn(Optional.of("h:deputy-1"));
        lenient().when(askRepository.findByToAndStatusPending(any())).thenReturn(List.of());

        AskService svc = buildService();
        svc.expireNow("ask-1", "admin-1");

        ArgumentCaptor<Ask> saved = ArgumentCaptor.forClass(Ask.class);
        verify(askRepository, atLeastOnce()).save(saved.capture());
        Ask successor = saved.getAllValues().stream()
            .filter(a -> !"ask-1".equals(a.getId()))
            .findFirst().orElseThrow();
        assertEquals("ask-1", successor.getEscalation());
        assertEquals("h:deputy-1", successor.getTo());
        assertEquals("escalate", successor.getExpiryBehavior());
    }

    @Test
    void expireNow_exhaustedChainBroadcastsOrgStallInsteadOfASuccessor() {
        // ASK-057: chain exhaustion applies on the manual door exactly as on the
        // sweep — the depth budget is shared, so a force-expiry cannot reset it.
        // An exhausted chain broadcasts the org-stall alert instead of opening
        // yet another successor.
        when(askRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(memberService.isAdmin("admin-1")).thenReturn(true);
        lenient().when(governanceService.getSetting(anyString(), eq(Number.class))).thenReturn(null);
        lenient().when(memberService.resolveDeputyTarget(any())).thenReturn(Optional.empty());
        lenient().when(memberService.findAdmins()).thenReturn(List.of());
        lenient().when(askRepository.findByToAndStatusPending(any())).thenReturn(List.of());

        for (int i = 0; i <= com.summa.constants.Defaults.MAX_EXPIRE_SUCCESSOR_DEPTH; i++) {
            Ask hop = new Ask();
            hop.setId("ask-hop-" + i);
            hop.setKind("question");
            hop.setStatus("pending");
            hop.setFrom("h:proposer");
            hop.setTo("h:owner-1");
            hop.setSlaTier("standard");
            hop.setExpiryBehavior("escalate");
            hop.setQuorumRequired(1);
            hop.setPayload("{}");
            hop.setEscalation("ask-root");
            hop.setDeadline(Instant.now().plusSeconds(3600));
            lenient().when(askRepository.findById("ask-hop-" + i)).thenReturn(Optional.of(hop));
        }

        AskService svc = buildService();
        for (int i = 0; i < com.summa.constants.Defaults.MAX_EXPIRE_SUCCESSOR_DEPTH; i++) {
            assertEquals("expired", svc.expireNow("ask-hop-" + i, "admin-1").getStatus());
        }
        verify(auditService, never()).logSystem(eq("ORG_STALL_BROADCAST"), anyString(), anyString(), anyString());

        // The budget is spent: the next hop exhausts the chain.
        assertEquals("expired", svc.expireNow("ask-hop-" + com.summa.constants.Defaults.MAX_EXPIRE_SUCCESSOR_DEPTH, "admin-1").getStatus());
        verify(auditService, times(com.summa.constants.Defaults.MAX_EXPIRE_SUCCESSOR_DEPTH))
            .log(eq("admin-1"), eq("EXPIRE_SUCCESSOR_CREATED"), eq("ask"), anyString(), anyString());
        verify(auditService).logSystem(eq("EXPIRE_CHAIN_EXHAUSTED"), eq("ask"),
            eq("ask-hop-" + com.summa.constants.Defaults.MAX_EXPIRE_SUCCESSOR_DEPTH), anyString());
        verify(auditService).logSystem(eq("ORG_STALL_BROADCAST"), eq("ask"),
            eq("ask-hop-" + com.summa.constants.Defaults.MAX_EXPIRE_SUCCESSOR_DEPTH), anyString());
    }

    // --- STG-031: the erasure sweep's responses-ledger pseudonymization ---

    @Test
    void pseudonymizeResponderInLedgers_rewritesBareAndKeyedEntries() {
        // The ledger keeps its event shape (text, decision) while both stored
        // forms of the member's responder identity sever behind the pseudonym.
        Ask bothForms = new Ask();
        bothForms.setId("ask-1");
        bothForms.setResponses("[{\"responder\":\"human-1\",\"response\":\"ok\",\"decision\":\"accept\"},"
            + "{\"responder\":\"h:human-1\",\"response\":\"again\",\"decision\":\"deny\"}]");
        when(askRepository.findByResponsesContaining(anyString())).thenReturn(List.of(bothForms));
        when(askRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AskService svc = buildService();
        int rewritten = svc.pseudonymizeResponderInLedgers("human-1", "erased-abc");

        assertEquals(1, rewritten);
        assertEquals("[{\"responder\":\"erased-abc\",\"response\":\"ok\",\"decision\":\"accept\"},"
            + "{\"responder\":\"erased-abc\",\"response\":\"again\",\"decision\":\"deny\"}]",
            bothForms.getResponses());
    }

    @Test
    void pseudonymizeResponderInLedgers_leavesOtherRespondersAlone() {
        Ask ledger = new Ask();
        ledger.setId("ask-1");
        ledger.setResponses("[{\"responder\":\"human-11\",\"response\":\"x\",\"decision\":\"accept\"},"
            + "{\"responder\":\"h:human-2\",\"response\":\"y\",\"decision\":\"accept\"}]");
        // The LIKE needles are substrings, so an id that merely contains the
        // erased id (human-11 ⊃ human-1) can match the query — the exact entry
        // re-check must keep the row untouched and unrewritten.
        when(askRepository.findByResponsesContaining(anyString())).thenReturn(List.of(ledger));

        AskService svc = buildService();
        int rewritten = svc.pseudonymizeResponderInLedgers("human-1", "erased-abc");

        assertEquals(0, rewritten);
        verify(askRepository, never()).save(any());
        assertTrue(ledger.getResponses().contains("human-11"));
        assertTrue(ledger.getResponses().contains("h:human-2"));
    }

    @Test
    void pseudonymizeResponderInLedgers_survivesAMalformedRow() {
        // One malformed ledger row must not abort the erasure sweep — the row
        // is named in an audit event and the sweep moves on.
        Ask malformed = new Ask();
        malformed.setId("ask-bad");
        malformed.setResponses("not-json");
        when(askRepository.findByResponsesContaining(anyString())).thenReturn(List.of(malformed));

        AskService svc = buildService();
        int rewritten = svc.pseudonymizeResponderInLedgers("human-1", "erased-abc");

        assertEquals(0, rewritten);
        verify(auditService).logSystem(eq("ERASURE_LEDGER_FAIL"), eq("ask"), eq("ask-bad"), anyString());
    }

    @Test
    void expireNow_walksTheDomainOwnerHopAfterTheDeputy() {
        // ASK-060: member -> deputy -> domain owner -> admin. With no live deputy,
        // the successor lands on the domain owner of the ask's workspace.
        Ask ask = new Ask();
        ask.setId("ask-domain-1");
        ask.setKind("question");
        ask.setStatus("pending");
        ask.setFrom("h:proposer");
        ask.setTo("h:owner-1");
        ask.setSlaTier("standard");
        ask.setExpiryBehavior("escalate");
        ask.setQuorumRequired(1);
        ask.setPayload("{}");
        ask.setWorkspaceId("ws-1");
        ask.setDeadline(Instant.now().plusSeconds(3600));
        when(askRepository.findById("ask-domain-1")).thenReturn(Optional.of(ask));
        when(askRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(memberService.isAdmin("admin-1")).thenReturn(true);
        lenient().when(governanceService.getSetting(anyString(), eq(Number.class))).thenReturn(null);
        lenient().when(memberService.resolveDeputyTarget("h:owner-1")).thenReturn(Optional.empty());
        lenient().when(askRepository.findByToAndStatusPending(any())).thenReturn(List.of());

        com.summa.model.Workspace ws = new com.summa.model.Workspace();
        ws.setId("ws-1");
        ws.setDomainIds("[\"domain-1\",\"domain-2\"]");
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(ws));
        com.summa.model.DnaDomain domain = new com.summa.model.DnaDomain();
        domain.setId("domain-1");
        domain.setStatus("active");
        domain.setOwnerHumanId("human-owner");
        when(domainRepository.findById("domain-1")).thenReturn(Optional.of(domain));
        Human owner = new Human();
        owner.setId("human-owner");
        owner.setRbac("member");
        owner.setDeactivatedAt(null);
        when(memberService.findHuman("human-owner")).thenReturn(Optional.of(owner));

        AskService svc = buildService();
        svc.expireNow("ask-domain-1", "admin-1");

        ArgumentCaptor<Ask> saved = ArgumentCaptor.forClass(Ask.class);
        verify(askRepository, atLeastOnce()).save(saved.capture());
        Ask successor = saved.getAllValues().stream()
            .filter(a -> !"ask-domain-1".equals(a.getId()))
            .findFirst().orElseThrow();
        assertEquals("h:human-owner", successor.getTo());
    }

    @Test
    void expireNow_neverCollapsesItsSuccessorBackIntoTheExpiringAsk() {
        // ASK-100 must not fold an expiry successor into the very ask that is
        // about to expire: that would create no successor and silently end the chain.
        Ask ask = new Ask();
        ask.setId("ask-self-1");
        ask.setKind("question");
        ask.setStatus("pending");
        ask.setFrom("h:proposer");
        ask.setTo("h:owner-1");
        ask.setSlaTier("standard");
        ask.setExpiryBehavior("escalate");
        ask.setQuorumRequired(1);
        ask.setPayload("{}");
        ask.setDeadline(Instant.now().plusSeconds(3600));
        when(askRepository.findById("ask-self-1")).thenReturn(Optional.of(ask));
        when(askRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(memberService.isAdmin("admin-1")).thenReturn(true);
        lenient().when(governanceService.getSetting(anyString(), eq(Number.class))).thenReturn(null);
        lenient().when(memberService.resolveDeputyTarget(any())).thenReturn(Optional.empty());
        // The original ask is the only pending candidate for the successor's key.
        lenient().when(askRepository.findByToAndStatusPending("admins")).thenReturn(List.of(ask));

        AskService svc = buildService();
        svc.expireNow("ask-self-1", "admin-1");

        ArgumentCaptor<Ask> saved = ArgumentCaptor.forClass(Ask.class);
        verify(askRepository, atLeastOnce()).save(saved.capture());
        assertTrue(saved.getAllValues().stream()
                .anyMatch(a -> !"ask-self-1".equals(a.getId()) && "admins".equals(a.getTo())));
        verify(auditService).log(eq("admin-1"), eq("EXPIRE_SUCCESSOR_CREATED"), eq("ask"),
            eq("ask-self-1"), anyString());
    }
}
