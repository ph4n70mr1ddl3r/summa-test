package com.summa.controller;

import com.summa.model.AuditEvent;
import com.summa.model.DnaDomain;
import com.summa.model.DnaGoal;
import com.summa.model.Human;
import com.summa.security.TestAuthContext;
import com.summa.security.WriteGate;
import com.summa.service.AuditService;
import com.summa.service.DnaDomainService;
import com.summa.service.DnaGoalService;
import com.summa.service.MemberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ORG-020 on the goal surface: creating a goal inside a domain is the domain
 * owner's or an admin's write. The authority is the caller's — the goal's
 * `owner` field names who the goal belongs to, not who may write it, so the
 * door must never be evaluated against the caller's own body.
 */
class DnaGoalControllerTest {

    private DnaGoalService goalService;
    private AuditService auditService;
    private MemberService memberService;
    private DnaDomainService domainService;
    private DnaGoalController controller;

    @BeforeEach
    void setUp() {
        goalService = mock(DnaGoalService.class);
        auditService = mock(AuditService.class);
        memberService = mock(MemberService.class);
        domainService = mock(DnaDomainService.class);
        controller = new DnaGoalController(goalService, auditService, new WriteGate(auditService),
                memberService, domainService);
        when(auditService.log(any(), any(), any(), any(), any())).thenReturn(newAuditEvent());
        when(auditService.logSystem(anyString(), anyString(), any(), any())).thenReturn(newAuditEvent());
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

    private DnaDomain domainOwnedBy(String ownerHumanId) {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setOwnerHumanId(ownerHumanId);
        return domain;
    }

    private Human human(String id, String rbac) {
        Human human = new Human();
        human.setId(id);
        human.setRbac(rbac);
        return human;
    }

    @Test
    void createGoal_nonOwnerInDomain_refused() {
        TestAuthContext.allowWrites("intruder");
        when(memberService.isAdmin("intruder")).thenReturn(false);
        when(domainService.findById("domain-1")).thenReturn(Optional.of(domainOwnedBy("owner-1")));
        when(memberService.findHuman("owner-1")).thenReturn(Optional.of(human("owner-1", "owner")));
        when(memberService.findAgent("owner-1")).thenReturn(Optional.empty());

        var res = controller.createGoal(Map.of(
                "statementMd", "Ship it",
                "owner", "owner-1",
                "domainId", "domain-1"));

        assertEquals(403, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("domain owner"));
        verify(goalService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGoal_nonOwnerClaimingDomainOwner_refused() {
        // The claim in the body names the domain owner — the door must still be
        // evaluated against the caller.
        TestAuthContext.allowWrites("intruder");
        when(memberService.isAdmin("intruder")).thenReturn(false);
        when(domainService.findById("domain-1")).thenReturn(Optional.of(domainOwnedBy("owner-1")));
        when(memberService.findHuman("owner-1")).thenReturn(Optional.of(human("owner-1", "owner")));
        when(memberService.findAgent("owner-1")).thenReturn(Optional.empty());

        var res = controller.createGoal(Map.of(
                "statementMd", "Ship it",
                "owner", "owner-1",
                "domainId", "domain-1"));

        assertEquals(403, res.getStatusCodeValue());
        verify(goalService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGoal_domainOwner_allowed() {
        TestAuthContext.allowWrites("owner-1");
        when(memberService.isAdmin("owner-1")).thenReturn(false);
        when(domainService.findById("domain-1")).thenReturn(Optional.of(domainOwnedBy("owner-1")));
        when(memberService.findHuman("owner-1")).thenReturn(Optional.of(human("owner-1", "owner")));
        when(memberService.findAgent("owner-1")).thenReturn(Optional.empty());
        DnaGoal goal = new DnaGoal();
        goal.setId("goal-1");
        when(goalService.create(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(goal);

        var res = controller.createGoal(Map.of(
                "statementMd", "Ship it",
                "owner", "owner-1",
                "domainId", "domain-1"));

        assertEquals(200, res.getStatusCodeValue());
        verify(goalService).create(anyString(), eq("domain-1"), isNull(), eq("Ship it"),
                eq("owner-1"), isNull(), any(), isNull(), eq("owner-1"));
    }

    @Test
    void createGoal_adminAllowed() {
        TestAuthContext.allowWrites("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        when(domainService.findById("domain-1")).thenReturn(Optional.of(domainOwnedBy("owner-1")));
        when(memberService.findHuman("owner-1")).thenReturn(Optional.of(human("owner-1", "owner")));
        when(memberService.findAgent("owner-1")).thenReturn(Optional.empty());
        DnaGoal goal = new DnaGoal();
        goal.setId("goal-1");
        when(goalService.create(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(goal);

        var res = controller.createGoal(Map.of(
                "statementMd", "Ship it",
                "owner", "owner-1",
                "domainId", "domain-1"));

        assertEquals(200, res.getStatusCodeValue());
    }

    @Test
    void createGoal_withAnUnknownInject_isAValidationRefusal() {
        // API-061: validation before constraint — dna_goals.inject carries a CHECK.
        TestAuthContext.allowWrites("owner-1");
        when(memberService.isAdmin("owner-1")).thenReturn(false);
        when(domainService.findById("domain-1")).thenReturn(Optional.of(domainOwnedBy("owner-1")));
        when(memberService.findHuman("owner-1")).thenReturn(Optional.of(human("owner-1", "owner")));
        when(memberService.findAgent("owner-1")).thenReturn(Optional.empty());

        var res = controller.createGoal(Map.of(
                "statementMd", "Ship it",
                "owner", "owner-1",
                "inject", "sometimes",
                "domainId", "domain-1"));

        assertEquals(422, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("inject"));
        verify(goalService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGoal_orgWide_nonAdmin_refused() {
        TestAuthContext.allowWrites("member-1");
        when(memberService.isAdmin("member-1")).thenReturn(false);
        when(memberService.findHuman("owner-1")).thenReturn(Optional.of(human("owner-1", "member")));
        when(memberService.findAgent("owner-1")).thenReturn(Optional.empty());

        var res = controller.createGoal(Map.of(
                "statementMd", "Org wide goal",
                "owner", "owner-1"));

        assertEquals(403, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("Admin access required for org-wide DNA write"));
        verify(goalService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGoal_orgWide_admin_allowed() {
        TestAuthContext.allowWrites("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        when(memberService.findHuman("owner-1")).thenReturn(Optional.of(human("owner-1", "member")));
        when(memberService.findAgent("owner-1")).thenReturn(Optional.empty());
        DnaGoal goal = new DnaGoal();
        goal.setId("goal-org");
        when(goalService.create(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(goal);

        var res = controller.createGoal(Map.of(
                "statementMd", "Org wide goal",
                "owner", "owner-1"));

        assertEquals(200, res.getStatusCodeValue());
        verify(goalService).create(anyString(), isNull(), isNull(), eq("Org wide goal"),
                eq("owner-1"), isNull(), any(), isNull(), eq("admin-1"));
    }

    @Test
    void createGoal_reviewerDistinctSod_refused() {
        TestAuthContext.allowWrites("owner-1");
        when(memberService.isAdmin("owner-1")).thenReturn(false);
        DnaDomain domain = domainOwnedBy("owner-1");
        domain.setSod("reviewer-distinct");
        when(domainService.findById("domain-1")).thenReturn(Optional.of(domain));
        when(memberService.findHuman("owner-1")).thenReturn(Optional.of(human("owner-1", "owner")));
        when(memberService.findAgent("owner-1")).thenReturn(Optional.empty());

        var res = controller.createGoal(Map.of(
                "statementMd", "Ship it",
                "owner", "owner-1",
                "domainId", "domain-1"));

        assertEquals(403, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("Separation of duties"));
        verify(goalService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGoal_missingDomain_returns404() {
        TestAuthContext.allowWrites("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        when(domainService.findById("domain-missing")).thenReturn(Optional.empty());
        when(memberService.findHuman("owner-1")).thenReturn(Optional.of(human("owner-1", "member")));
        when(memberService.findAgent("owner-1")).thenReturn(Optional.empty());

        var res = controller.createGoal(Map.of(
                "statementMd", "Ship it",
                "owner", "owner-1",
                "domainId", "domain-missing"));

        assertEquals(404, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("Domain not found"));
        verify(goalService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
