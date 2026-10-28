package com.summa.controller;

import com.summa.model.AuditEvent;
import com.summa.model.DnaCard;
import com.summa.model.DnaDomain;
import com.summa.model.Human;
import com.summa.security.TestAuthContext;
import com.summa.security.WriteGate;
import com.summa.service.AuditService;
import com.summa.service.DnaCardService;
import com.summa.service.DnaDomainService;
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
 * DWP-060 & SEC-020: Item-level DNA writes are restricted to the domain owner or an admin.
 * Non-owners in a domain cannot directly write cards; reviewer-distinct domains require proposals.
 */
class DnaCardControllerTest {

    private DnaCardService cardService;
    private AuditService auditService;
    private MemberService memberService;
    private DnaDomainService domainService;
    private DnaCardController controller;

    @BeforeEach
    void setUp() {
        cardService = mock(DnaCardService.class);
        auditService = mock(AuditService.class);
        memberService = mock(MemberService.class);
        domainService = mock(DnaDomainService.class);
        controller = new DnaCardController(cardService, auditService, new WriteGate(auditService),
                domainService, memberService);
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

    private DnaDomain domainOwnedBy(String ownerHumanId, String sod) {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setOwnerHumanId(ownerHumanId);
        domain.setSod(sod);
        return domain;
    }

    private Human human(String id, String rbac) {
        Human human = new Human();
        human.setId(id);
        human.setRbac(rbac);
        return human;
    }

    @Test
    void createCard_nonOwnerInDomain_refused() {
        TestAuthContext.allowWrites("intruder");
        when(memberService.isAdmin("intruder")).thenReturn(false);
        when(domainService.findById("domain-1")).thenReturn(Optional.of(domainOwnedBy("owner-1", "none")));
        when(memberService.findHuman("owner-1")).thenReturn(Optional.of(human("owner-1", "owner")));

        var res = controller.createCard(Map.of(
                "title", "Card 1",
                "definitionMd", "Definitions",
                "domainId", "domain-1"));

        assertEquals(403, res.getStatusCodeValue());
        verify(cardService, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createCard_domainWithReviewerDistinctSod_refusesDirectWrite() {
        TestAuthContext.allowWrites("owner-1");
        when(memberService.isAdmin("owner-1")).thenReturn(false);
        when(domainService.findById("domain-1")).thenReturn(Optional.of(domainOwnedBy("owner-1", "reviewer-distinct")));
        when(memberService.findHuman("owner-1")).thenReturn(Optional.of(human("owner-1", "owner")));

        var res = controller.createCard(Map.of(
                "title", "Card 1",
                "definitionMd", "Definitions",
                "domainId", "domain-1"));

        assertEquals(403, res.getStatusCodeValue());
        verify(cardService, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createCard_domainOwner_succeeds() {
        TestAuthContext.allowWrites("owner-1");
        when(memberService.isAdmin("owner-1")).thenReturn(false);
        when(domainService.findById("domain-1")).thenReturn(Optional.of(domainOwnedBy("owner-1", "none")));
        when(memberService.findHuman("owner-1")).thenReturn(Optional.of(human("owner-1", "owner")));

        DnaCard card = new DnaCard();
        card.setId("card-1");
        when(cardService.create(any(), eq("domain-1"), eq("Card 1"), eq("Definitions"), any(), eq("owner-1")))
                .thenReturn(card);

        var res = controller.createCard(Map.of(
                "title", "Card 1",
                "definitionMd", "Definitions",
                "domainId", "domain-1"));

        assertEquals(200, res.getStatusCodeValue());
        verify(cardService).create(any(), eq("domain-1"), eq("Card 1"), eq("Definitions"), any(), eq("owner-1"));
    }
}
