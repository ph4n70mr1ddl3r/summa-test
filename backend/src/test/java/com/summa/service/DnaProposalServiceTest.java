package com.summa.service;

import com.summa.repository.DnaProposalRepository;
import com.summa.repository.DnaRuleRepository;
import com.summa.model.DnaProposal;
import com.summa.model.DnaDomain;
import com.summa.model.DnaRule;
import com.summa.model.Ask;
import com.summa.model.Agent;
import com.summa.model.Human;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DnaProposalServiceTest {

    @Mock
    private DnaProposalRepository proposalRepository;

    @Mock
    private DnaRuleRepository ruleRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private DnaDomainService domainService;

    @Mock
    private MemberService memberService;

    @Mock
    private AskService askService;

    @Mock
    private GovernanceService governanceService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private DnaProposalService proposalService;

    @BeforeEach
    void setUp() {
        proposalService = new DnaProposalService(
            proposalRepository, ruleRepository, auditService,
            domainService, memberService, askService, governanceService, objectMapper
        );
    }

    @Test
    void create_proposalSetsDefaults() {
        when(proposalRepository.save(any())).thenAnswer(invocation -> {
            DnaProposal p = invocation.getArgument(0);
            if (p.getStatus() == null) p.setStatus("open");
            return p;
        });
        when(memberService.toKeyedMemberRef("a:agent-1")).thenReturn("a:agent-1");
        Agent agent = new Agent();
        agent.setId("agent-1");
        agent.setAgentClass("persistent");
        when(memberService.findAgent("agent-1")).thenReturn(Optional.of(agent));

        DnaProposal result = proposalService.create("prop-1", "card", "{}", "a:agent-1", "{}", "domain-1");

        assertNotNull(result);
        assertEquals("open", result.getStatus());
        assertNull(result.getReviewedAt());
        // DAT-120: proposed_by is stored in the keyed-union form
        assertEquals("a:agent-1", result.getProposedBy());
    }

    @Test
    void create_storesReviewingOwnerAsHumanReference() {
        when(proposalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberService.toKeyedMemberRef("h:human-1")).thenReturn("h:human-1");
        Human human = new Human();
        human.setId("human-1");
        human.setRbac("member");
        when(memberService.findHuman("human-1")).thenReturn(Optional.of(human));

        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setOwnerHumanId("human-1");
        when(domainService.findById("domain-1")).thenReturn(Optional.of(domain));

        DnaProposal result = proposalService.create("prop-1", "card", "{}", "h:human-1", "{}", "domain-1");

        // review_by is a humans(id) reference — the reserved 'admins' broadcast is
        // legal on asks.to alone and would abort the insert here.
        assertEquals("human-1", result.getReviewBy());
    }

    @Test
    void create_orgScopedReviewByIsAnAdminHumanNotTheBroadcast() {
        when(proposalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberService.toKeyedMemberRef("h:human-1")).thenReturn("h:human-1");
        Human human = new Human();
        human.setId("human-1");
        human.setRbac("member");
        when(memberService.findHuman("human-1")).thenReturn(Optional.of(human));

        Human admin = new Human();
        admin.setId("admin-1");
        when(memberService.findAdmins()).thenReturn(List.of(admin));

        DnaProposal result = proposalService.create("prop-1", "card", "{}", "h:human-1", "{}", null);

        assertEquals("admin-1", result.getReviewBy());
    }

    @Test
    void create_refusesUnknownProposer() {
        when(memberService.toKeyedMemberRef("h:ghost")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () ->
            proposalService.create("prop-1", "card", "{}", "h:ghost", "{}", null));
    }

    @Test
    void create_refusesViewerProposer() {
        // DWP-010: proposed_by must hold a write surface — viewers never propose.
        when(memberService.toKeyedMemberRef("h:viewer-1")).thenReturn("h:viewer-1");
        Human viewer = new Human();
        viewer.setId("viewer-1");
        viewer.setRbac("viewer");
        when(memberService.findHuman("viewer-1")).thenReturn(Optional.of(viewer));
        when(memberService.isViewer(viewer)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
            proposalService.create("prop-1", "card", "{}", "h:viewer-1", "{}", null));
    }

    @Test
    void publish_updatesStatus() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        proposal.setReviewBy("human-1");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenReturn(proposal);

        DnaProposal result = proposalService.publish("prop-1", "human-1", "human-1");

        assertEquals("published", result.getStatus());
        assertNotNull(result.getReviewedAt());
    }

    @Test
    void publish_refusesNonReviewingMember() {
        // DWP-003: the publish door belongs to the domain owner (review_by) or an admin.
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        proposal.setReviewBy("human-1");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));
        when(memberService.isAdmin("intruder")).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> {
            proposalService.publish("prop-1", "intruder", "intruder");
        });
    }

    @Test
    void publish_allowsAdmin() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        proposal.setReviewBy("human-1");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenReturn(proposal);
        when(memberService.isAdmin("admin-1")).thenReturn(true);

        DnaProposal result = proposalService.publish("prop-1", "admin-1", "admin-1");

        assertEquals("published", result.getStatus());
    }

    @Test
    void publish_separationOfDutiesRoutesProposerToAdmin() {
        // DWP-050: on a sod domain the proposer cannot be the publisher — the
        // publish routes to the admin broadcast (ASK-055) and the proposal stays
        // in review. The comparison is on the stripped forms.
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        proposal.setKind("card");
        proposal.setProposedBy("h:human-1");
        proposal.setReviewBy("human-1");
        proposal.setDomainId("domain-1");
        proposal.setPayload("{}");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));

        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setSod("reviewer-distinct");
        when(domainService.findById("domain-1")).thenReturn(Optional.of(domain));

        when(askService.deriveDeadlineFromTier(anyString())).thenReturn(3600L);

        IllegalStateException refusal = assertThrows(IllegalStateException.class, () ->
            proposalService.publish("prop-1", "human-1", "human-1"));
        assertTrue(refusal.getMessage().contains("Separation of duties"));

        // The publish is routed to the admin broadcast; the proposal stays open.
        verify(askService).create(eq("approval"), eq("system"),
            eq(OffboardingWalkService.ADMIN_BROADCAST), anyString(), eq("standard"), eq("deny"),
            eq(1), any(Instant.class), isNull(), isNull());
        assertEquals("open", proposal.getStatus());
    }

    @Test
    void publish_throwsWhenNotOpen() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("rejected");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));

        assertThrows(IllegalStateException.class, () -> {
            proposalService.publish("prop-1", "h:human-1", "h:human-1");
        });
    }

    @Test
    void withdraw_onlyProposerAllowed() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        proposal.setProposedBy("a:agent-1");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));

        assertThrows(IllegalArgumentException.class, () -> {
            proposalService.withdraw("prop-1", "agent-2");
        });
    }

    @Test
    void withdraw_allowedForKeyedProposer() {
        // DAT-120: proposed_by is a keyed union and the actor is a bare id — the
        // proposer must still be recognized across the two spellings.
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        proposal.setProposedBy("h:human-1");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenReturn(proposal);

        DnaProposal result = proposalService.withdraw("prop-1", "human-1");

        assertEquals("withdrawn", result.getStatus());
    }

    @Test
    void withdraw_refusesSettledProposal() {
        // DWP-051: withdrawal is for a pending proposal — a published one is
        // settled history and must not be retractable.
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("published");
        proposal.setProposedBy("h:human-1");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));

        assertThrows(IllegalStateException.class, () -> {
            proposalService.withdraw("prop-1", "human-1");
        });
    }

    @Test
    void amend_incrementsRevision() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        proposal.setRevision(1);
        proposal.setProposedBy("a:agent-1");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenReturn(proposal);

        DnaProposal result = proposalService.amend("prop-1", "{\"new\":\"payload\"}", "a:agent-1");

        assertEquals(2, result.getRevision());
    }

    @Test
    void amend_allowedForReviewingOwner() {
        // DWP-030: the reviewing owner may file a suggested change.
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        proposal.setRevision(1);
        proposal.setProposedBy("h:human-1");
        proposal.setReviewBy("owner-1");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenReturn(proposal);

        DnaProposal result = proposalService.amend("prop-1", "{\"new\":\"payload\"}", "owner-1");

        assertEquals(2, result.getRevision());
    }

    @Test
    void amend_refusesUnrelatedMember() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        proposal.setRevision(1);
        proposal.setProposedBy("h:human-1");
        proposal.setReviewBy("owner-1");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));
        when(memberService.isAdmin("intruder")).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> {
            proposalService.amend("prop-1", "{}", "intruder");
        });
    }

    @Test
    void amend_throwsWhenNotOpen() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("published");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));

        assertThrows(IllegalStateException.class, () -> {
            proposalService.amend("prop-1", "{}", "a:agent-1");
        });
    }

    @Test
    void publish_succeedsWhenNoContradictoryRule() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        proposal.setKind("rule");
        proposal.setDomainId("domain-1");
        proposal.setPayload("{\"supersedes_id\":\"rule-old\"}");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));
        JsonNode payload;
        try {
            payload = new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"supersedes_id\":\"rule-old\"}");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            doReturn(payload).when(objectMapper).readTree(anyString());
        } catch (Exception ignored) {
            // readTree throws checked exception; doReturn bypasses invocation
        }
        when(ruleRepository.findBySupersedesId("rule-old")).thenReturn(List.of());
        DnaRule supersedesRule = new DnaRule();
        supersedesRule.setId("rule-old");
        when(ruleRepository.findById("rule-old")).thenReturn(Optional.of(supersedesRule));
        when(proposalRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        proposal.setReviewBy("human-1");

        DnaProposal result = proposalService.publish("prop-1", "human-1", "human-1");
        assertNotNull(result);
        assertEquals("published", result.getStatus());
    }

    @Test
    void checkAndEscalateBreachedProposals_escalatesOldProposal() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        proposal.setKind("card");
        proposal.setDomainId("domain-1");
        proposal.setCreatedAt(Instant.now().minusSeconds(8L * 86400L)); // 8 days ago, SLA is 7

        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setReviewSlaDays(7);
        when(proposalRepository.findAllOpen()).thenReturn(List.of(proposal));
        when(domainService.findById("domain-1")).thenReturn(Optional.of(domain));

        Ask ask = new Ask();
        ask.setId("ask-escalation");
        when(askService.create(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), any(), anyString(), anyString())).thenReturn(ask);

        proposalService.checkAndEscalateBreachedProposals();

        verify(askService).create(eq("question"), eq("system"), eq("admins"), anyString(), eq("critical"), eq("escalate"), eq(1), any(), isNull(), isNull());
    }

    @Test
    void checkAndEscalateBreachedProposals_omitsNonBreached() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        proposal.setKind("card");
        proposal.setDomainId("domain-1");
        proposal.setCreatedAt(Instant.now().minusSeconds(1L * 86400L)); // 1 day ago, SLA is 7

        when(proposalRepository.findAllOpen()).thenReturn(List.of(proposal));
        DnaDomain domain = new DnaDomain();
        domain.setReviewSlaDays(7);
        when(domainService.findById("domain-1")).thenReturn(Optional.of(domain));

        proposalService.checkAndEscalateBreachedProposals();

        verify(askService, never()).create(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyInt(), any(), anyString(), anyString());
    }

    @Test
    void publish_withNullProposedByDoesNotNpe() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        proposal.setKind("rule");
        proposal.setPayload("{\"statement\":\"test\"}");
        proposal.setProposedBy(null);
        proposal.setDomainId(null);
        proposal.setReviewBy("human-1");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Should not throw NPE — proposedBy is null so SoD check is skipped
        assertDoesNotThrow(() -> proposalService.publish("prop-1", "human-1", "human-1"));
    }
}
