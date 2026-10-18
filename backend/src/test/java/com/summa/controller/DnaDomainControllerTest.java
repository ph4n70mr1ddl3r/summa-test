package com.summa.controller;

import com.summa.enums.RbacRole;
import com.summa.model.DnaDomain;
import com.summa.model.Human;
import com.summa.model.AuditEvent;
import com.summa.security.TestAuthContext;
import com.summa.security.WriteGate;
import com.summa.service.DnaDomainService;
import com.summa.service.AuditService;
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
 * DGV-005 / API-020 / API-023 row-write authority gates on the domain surface:
 * create/archive/owner re-pointing/rename/split/merge are admin writes; the
 * owner-editable attributes (`access`, `review_sla_days`) require the domain
 * owner — holding role `owner` or `admin` at write (DGV-006) — or an admin.
 */
class DnaDomainControllerTest {

    private DnaDomainService domainService;
    private AuditService auditService;
    private MemberService memberService;
    private DnaDomainController controller;

    @BeforeEach
    void setUp() {
        domainService = mock(DnaDomainService.class);
        auditService = mock(AuditService.class);
        memberService = mock(MemberService.class);
        controller = new DnaDomainController(domainService, auditService, new WriteGate(auditService), memberService);
        // gate()/validation() refusal envelopes embed an audit event id — never null
        when(auditService.log(any(), any(), any(), any(), any())).thenReturn(newAuditEvent());
        when(auditService.logSystem(anyString(), anyString(), any(), any())).thenReturn(newAuditEvent());
    }

    @AfterEach
    void tearDown() {
        TestAuthContext.clear();
    }

    private void asWriter(String actor) {
        TestAuthContext.allowWrites(actor);
    }

    private DnaDomain ownedDomain() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setName("Core");
        domain.setOwnerHumanId("owner-1");
        domain.setAccess("public");
        return domain;
    }

    private Human humanWithRole(String rbac) {
        Human human = new Human();
        human.setId("owner-1");
        human.setRbac(rbac);
        return human;
    }

    // --- rename / split / merge: admin-only topology reorgs -----------------

    @Test
    void rename_byNonAdmin_refused() {
        asWriter("user-1");
        when(memberService.isAdmin("user-1")).thenReturn(false);

        var res = controller.renameDomain("domain-1", Map.of("name", "Renamed"));

        assertEquals(403, res.getStatusCodeValue());
        assertEquals("Admin access required to rename domains", bodyMessage(res));
        verify(domainService, never()).rename(any(), any(), any());
    }

    @Test
    void rename_byAdmin_allowed() {
        asWriter("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        when(domainService.rename(eq("domain-1"), eq("Renamed"), eq("admin-1"))).thenReturn(ownedDomain());

        var res = controller.renameDomain("domain-1", Map.of("name", "Renamed"));

        assertEquals(200, res.getStatusCodeValue());
        verify(domainService).rename("domain-1", "Renamed", "admin-1");
    }

    @Test
    void split_byNonAdmin_refused() {
        asWriter("user-1");
        when(memberService.isAdmin("user-1")).thenReturn(false);

        var res = controller.splitDomain("domain-1", Map.of());

        assertEquals(403, res.getStatusCodeValue());
        assertEquals("Admin access required to split domains", bodyMessage(res));
        verify(domainService, never()).split(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void merge_byNonAdmin_refused() {
        asWriter("user-1");
        when(memberService.isAdmin("user-1")).thenReturn(false);

        var res = controller.mergeDomain("domain-1", Map.of("sourceId", "domain-2"));

        assertEquals(403, res.getStatusCodeValue());
        assertEquals("Admin access required to merge domains", bodyMessage(res));
        verify(domainService, never()).merge(any(), any(), any(), any(), any());
    }

    // --- access / review-sla: domain owner (role-checked) or admin ----------

    @Test
    void access_byUnrelatedMember_refused() {
        asWriter("user-1");
        when(memberService.isAdmin("user-1")).thenReturn(false);
        when(domainService.findById("domain-1")).thenReturn(Optional.of(ownedDomain()));

        var res = controller.updateAccess("domain-1", Map.of("access", "named"));

        assertEquals(403, res.getStatusCodeValue());
        assertEquals("Only the domain owner or an admin may edit this domain attribute", bodyMessage(res));
        verify(domainService, never()).updateAccess(any(), any(), any());
    }

    @Test
    void access_byDomainOwnerHoldingOwnerRole_allowed() {
        asWriter("owner-1");
        when(memberService.isAdmin("owner-1")).thenReturn(false);
        when(domainService.findById("domain-1")).thenReturn(Optional.of(ownedDomain()));
        when(memberService.findHuman("owner-1")).thenReturn(Optional.of(humanWithRole("owner")));
        when(domainService.updateAccess("domain-1", "named", "owner-1")).thenReturn(ownedDomain());

        var res = controller.updateAccess("domain-1", Map.of("access", "named"));

        assertEquals(200, res.getStatusCodeValue());
        verify(domainService).updateAccess("domain-1", "named", "owner-1");
    }

    @Test
    void access_byDomainOwnerDemotedBelowOwnerRole_refused() {
        // DGV-006: the owner must hold role `owner` or `admin` at write
        asWriter("owner-1");
        when(memberService.isAdmin("owner-1")).thenReturn(false);
        when(domainService.findById("domain-1")).thenReturn(Optional.of(ownedDomain()));
        when(memberService.findHuman("owner-1")).thenReturn(Optional.of(humanWithRole("member")));

        var res = controller.updateAccess("domain-1", Map.of("access", "named"));

        assertEquals(403, res.getStatusCodeValue());
        verify(domainService, never()).updateAccess(any(), any(), any());
    }

    @Test
    void access_byAdmin_allowed() {
        asWriter("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        when(domainService.updateAccess("domain-1", "domain", "admin-1")).thenReturn(ownedDomain());

        var res = controller.updateAccess("domain-1", Map.of("access", "domain"));

        assertEquals(200, res.getStatusCodeValue());
        verify(domainService).updateAccess("domain-1", "domain", "admin-1");
        // No domain lookup needed on the admin fast path
        verify(domainService, never()).findById(any());
    }

    @Test
    void access_onMissingDomain_fallsThroughToNotFound() {
        asWriter("owner-1");
        when(memberService.isAdmin("owner-1")).thenReturn(false);
        when(domainService.findById("domain-404")).thenReturn(Optional.empty());
        when(domainService.updateAccess(eq("domain-404"), any(), eq("owner-1")))
            .thenThrow(new com.summa.exception.EntityNotFoundException("Domain not found: domain-404"));

        assertThrows(com.summa.exception.EntityNotFoundException.class,
            () -> controller.updateAccess("domain-404", Map.of("access", "named")));
    }

    @Test
    void reviewSla_byDomainOwner_allowed() {
        asWriter("owner-1");
        when(memberService.isAdmin("owner-1")).thenReturn(false);
        when(domainService.findById("domain-1")).thenReturn(Optional.of(ownedDomain()));
        when(memberService.findHuman("owner-1")).thenReturn(Optional.of(humanWithRole("owner")));
        DnaDomain updated = ownedDomain();
        updated.setReviewSlaDays(3);
        when(domainService.updateReviewSlaDays("domain-1", 3, "owner-1")).thenReturn(updated);

        var res = controller.updateReviewSla("domain-1", Map.of("reviewSlaDays", "3"));

        assertEquals(200, res.getStatusCodeValue());
        verify(domainService).updateReviewSlaDays("domain-1", 3, "owner-1");
    }

    @Test
    void reviewSla_byUnrelatedMember_refused() {
        asWriter("user-1");
        when(memberService.isAdmin("user-1")).thenReturn(false);
        when(domainService.findById("domain-1")).thenReturn(Optional.of(ownedDomain()));

        var res = controller.updateReviewSla("domain-1", Map.of("reviewSlaDays", "3"));

        assertEquals(403, res.getStatusCodeValue());
        verify(domainService, never()).updateReviewSlaDays(any(), any(), any());
    }

    @Test
    void rbacRoleHelper_coversDgv006Split() {
        // DGV-006's "role owner or admin at write" is exactly canManageDomains()
        assertTrue(RbacRole.OWNER.canManageDomains());
        assertTrue(RbacRole.ADMIN.canManageDomains());
        assertFalse(RbacRole.MEMBER.canManageDomains());
        assertFalse(RbacRole.VIEWER.canManageDomains());
    }

    @SuppressWarnings("unchecked")
    private static String bodyMessage(Object res) {
        Object body = ((org.springframework.http.ResponseEntity<Object>) res).getBody();
        return (String) ((Map<String, Object>) body).get("message");
    }

    private static AuditEvent newAuditEvent() {
        AuditEvent e = new AuditEvent();
        e.setId("audit-" + System.nanoTime());
        return e;
    }
}
