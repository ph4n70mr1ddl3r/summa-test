package com.summa.controller;

import com.summa.model.AuditEvent;
import com.summa.model.DnaDomain;
import com.summa.model.DnaProposal;
import com.summa.security.TestAuthContext;
import com.summa.service.AuditService;
import com.summa.service.DnaDomainService;
import com.summa.service.DnaProposalService;
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
 * DWP-021/DWP-022/DGV-004 on the review queue.
 *
 * <p>The queue is the domain's (rendering to whoever holds the domain's
 * {@code owner_human_id}) with the org-wide queue an admin surface. The door
 * refused the org-wide queue for everyone and admitted the domain queue only to
 * the domain's owner — so an admin had no path to the review queue at all, and
 * the console's request for it was a permanent 403.
 */
class DnaProposalControllerTest {

    private DnaProposalService proposalService;
    private DnaDomainService domainService;
    private AuditService auditService;
    private MemberService memberService;
    private DnaProposalController controller;

    @BeforeEach
    void setUp() {
        proposalService = mock(DnaProposalService.class);
        domainService = mock(DnaDomainService.class);
        auditService = mock(AuditService.class);
        memberService = mock(MemberService.class);
        controller = new DnaProposalController(proposalService, auditService,
                new com.summa.security.WriteGate(auditService), memberService, domainService);
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

    private DnaProposal proposal() {
        DnaProposal p = new DnaProposal();
        p.setId("proposal-1");
        p.setStatus("open");
        return p;
    }

    @Test
    void reviewQueue_orgWideQueue_isAdminReadable() {
        // DWP-022: org-scoped items route to the admin review queue.
        TestAuthContext.allowWrites("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        when(proposalService.findAllOpen()).thenReturn(List.of(proposal()));

        var res = controller.reviewQueue(null);

        assertEquals(200, res.getStatusCodeValue());
        verify(proposalService).findAllOpen();
    }

    @Test
    void reviewQueue_orgWideQueue_refusedForANonAdmin() {
        TestAuthContext.allowWrites("member-1");
        when(memberService.isAdmin("member-1")).thenReturn(false);

        var res = controller.reviewQueue(null);

        assertEquals(403, res.getStatusCodeValue());
        verify(proposalService, never()).findAllOpen();
    }

    @Test
    void reviewQueue_domainQueue_admitsTheDomainOwner() {
        // DWP-021: the queue belongs to the domain and renders to whoever holds
        // the domain's owner_human_id.
        TestAuthContext.allowWrites("owner-1");
        when(memberService.isAdmin("owner-1")).thenReturn(false);
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setOwnerHumanId("owner-1");
        domain.setStatus("active");
        when(domainService.findById("domain-1")).thenReturn(Optional.of(domain));
        when(proposalService.findOpenByDomain("domain-1")).thenReturn(List.of(proposal()));

        var res = controller.reviewQueue("domain-1");

        assertEquals(200, res.getStatusCodeValue());
        verify(proposalService).findOpenByDomain("domain-1");
    }

    @Test
    void reviewQueue_domainQueue_admitsAnAdmin() {
        // DGV-004: an active admin holds a governance read of every domain.
        TestAuthContext.allowWrites("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setOwnerHumanId("owner-1");
        domain.setStatus("active");
        when(domainService.findById("domain-1")).thenReturn(Optional.of(domain));
        when(proposalService.findOpenByDomain("domain-1")).thenReturn(List.of(proposal()));

        var res = controller.reviewQueue("domain-1");

        assertEquals(200, res.getStatusCodeValue());
        verify(proposalService).findOpenByDomain("domain-1");
    }

    @Test
    void reviewQueue_domainQueue_refusedForAnUnrelatedMember() {
        TestAuthContext.allowWrites("member-1");
        when(memberService.isAdmin("member-1")).thenReturn(false);
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setOwnerHumanId("owner-1");
        domain.setStatus("active");
        when(domainService.findById("domain-1")).thenReturn(Optional.of(domain));

        var res = controller.reviewQueue("domain-1");

        assertEquals(403, res.getStatusCodeValue());
        verify(proposalService, never()).findOpenByDomain(any());
    }
}
