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

import java.util.List;
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

    // --- owner assignment: DGV-006's role requirement at write ---------------

    @Test
    void createDomain_withAnOwnerLackingTheDomainRole_refused() {
        // DGV-006: the owner must hold role `owner` or `admin` at write. A
        // domain owned by a plain member locks its own owner out of the owner's
        // attribute doors, which admit only an owner/admin holder.
        asWriter("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        when(memberService.findHuman("member-1")).thenReturn(Optional.of(humanWithId("member-1", "member")));

        var res = controller.createDomain(Map.of(
                "name", "Core",
                "ownerHumanId", "member-1"));

        assertEquals(403, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("owner or admin"));
        verify(domainService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createDomain_withAViewerOwner_refused() {
        // ORG-020: a viewer is never a domain owner.
        asWriter("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        when(memberService.findHuman("viewer-1")).thenReturn(Optional.of(humanWithId("viewer-1", "viewer")));
        when(memberService.isViewer(any())).thenReturn(true);

        var res = controller.createDomain(Map.of(
                "name", "Core",
                "ownerHumanId", "viewer-1"));

        assertEquals(403, res.getStatusCodeValue());
        verify(domainService, never()).create(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createDomain_withAnOwnerHoldingTheOwnerRole_allowed() {
        asWriter("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        when(memberService.findHuman("owner-1")).thenReturn(Optional.of(humanWithRole("owner")));
        when(domainService.create(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(ownedDomain());

        var res = controller.createDomain(Map.of(
                "name", "Core",
                "ownerHumanId", "h:owner-1"));

        assertEquals(200, res.getStatusCodeValue());
        verify(domainService).create(any(), eq("Core"), eq("owner-1"), any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateOwner_withABlankOwner_refused() {
        // The column is NOT NULL — a blank owner must be a 422, not a 409 abort.
        asWriter("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);

        var res = controller.updateOwner("domain-1", Map.of("ownerHumanId", "  "));

        assertEquals(422, res.getStatusCodeValue());
        verify(domainService, never()).updateOwner(any(), any(), any());
    }

    @Test
    void updateOwner_withAnUnknownOwner_refused() {
        asWriter("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        when(memberService.findHuman("ghost")).thenReturn(Optional.empty());

        var res = controller.updateOwner("domain-1", Map.of("ownerHumanId", "ghost"));

        assertEquals(422, res.getStatusCodeValue());
        verify(domainService, never()).updateOwner(any(), any(), any());
    }

    @Test
    void updateOwner_withAnOwnerLackingTheDomainRole_refused() {
        asWriter("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        when(memberService.findHuman("member-1")).thenReturn(Optional.of(humanWithId("member-1", "member")));

        var res = controller.updateOwner("domain-1", Map.of("ownerHumanId", "member-1"));

        assertEquals(403, res.getStatusCodeValue());
        verify(domainService, never()).updateOwner(any(), any(), any());
    }

    @Test
    void split_withAnOwnerLackingTheDomainRole_refused() {
        // DGV-011/DGV-006: a split names each result's owner, and the named
        // owner must be able to hold the post.
        asWriter("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        when(memberService.findHuman("member-1")).thenReturn(Optional.of(humanWithId("member-1", "member")));

        var res = controller.splitDomain("domain-1", Map.of("ownerHumanId", "member-1"));

        assertEquals(403, res.getStatusCodeValue());
        verify(domainService, never()).split(any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any());
    }

    @Test
    void split_storesTheBareOwnerId() {
        // `dna_domains.owner_human_id` is foreign-key-pinned to humans(id), so a
        // keyed union would abort the insert on the foreign key.
        asWriter("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        when(memberService.findHuman("owner-1")).thenReturn(Optional.of(humanWithRole("owner")));
        when(domainService.split(eq("domain-1"), eq("admin-1"), eq("owner-1"), any(), any(), any(),
                any(), any(), any(), any(), any())).thenReturn(List.of(ownedDomain()));

        var res = controller.splitDomain("domain-1", Map.of("ownerHumanId", "h:owner-1"));

        assertEquals(200, res.getStatusCodeValue());
        verify(domainService).split(eq("domain-1"), eq("admin-1"), eq("owner-1"), any(), any(), any(),
                any(), any(), any(), any(), any());
    }

    @Test
    void access_withABlankValue_refused() {
        // `dna_domains.access` is NOT NULL with a CHECK on the three policies —
        // an absent value must be a 422, not a 409 abort.
        asWriter("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);

        var res = controller.updateAccess("domain-1", Map.of());

        assertEquals(422, res.getStatusCodeValue());
        verify(domainService, never()).updateAccess(any(), any(), any());
    }

    private Human humanWithId(String id, String rbac) {
        Human human = new Human();
        human.setId(id);
        human.setRbac(rbac);
        return human;
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
