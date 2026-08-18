package com.summa.service;

import com.summa.repository.AgentRepository;
import com.summa.repository.GroupMembershipRepository;
import com.summa.repository.PatRepository;
import com.summa.repository.BoardTaskRepository;
import com.summa.repository.InitiativeRepository;
import com.summa.repository.DnaGoalRepository;
import com.summa.repository.DnaProposalRepository;
import com.summa.repository.AskRepository;
import com.summa.model.Human;
import com.summa.model.Agent;
import com.summa.model.Initiative;
import com.summa.model.DnaGoal;
import com.summa.model.DnaProposal;
import com.summa.model.DnaDomain;
import com.summa.model.BoardTask;
import com.summa.model.Pat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffboardingWalkServiceTest {

    @Mock private MemberService memberService;
    @Mock private AgentService agentService;
    @Mock private InitiativeService initiativeService;
    @Mock private BoardTaskService boardTaskService;
    @Mock private DnaProposalService proposalService;
    @Mock private AuditService auditService;
    @Mock private AskService askService;
    @Mock private SpawnService spawnService;
    @Mock private DnaDomainService domainService;
    @Mock private DnaGoalService goalService;
    @Mock private GroupMembershipRepository groupMembershipRepository;
    @Mock private AgentRepository agentRepository;
    @Mock private InitiativeRepository initiativeRepository;
    @Mock private DnaGoalRepository goalRepository;
    @Mock private DnaProposalRepository proposalRepository;
    @Mock private AskRepository askRepository;
    @Mock private BoardTaskRepository boardTaskRepository;
    @Mock private PatRepository patRepository;

    @InjectMocks
    private OffboardingWalkService walkService;

    @Test
    void walkOffboard_throwsWhenHumanNotFound() {
        when(memberService.findHuman("h1")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> {
            walkService.walkOffboard("h1", null, "admin2");
        });
    }

    @Test
    void walkOffboard_deactivatesHuman() {
        Human human = new Human();
        human.setId("h1");
        human.setRbac("member");
        human.setDeactivatedAt(null);
        when(memberService.findHuman("h1")).thenReturn(Optional.of(human));
        when(memberService.saveHuman(any())).thenReturn(human);
        when(domainService.findAllIncludingArchived()).thenReturn(java.util.List.of());
        when(agentService.findByOwner("h1")).thenReturn(java.util.List.of());
        when(initiativeService.findAllActive()).thenReturn(java.util.List.of());
        when(goalService.findAllActiveWindowed(any())).thenReturn(java.util.List.of());
        when(memberService.findAllActiveHumans()).thenReturn(java.util.List.of());
        when(proposalService.findAllOpen()).thenReturn(java.util.List.of());
        when(groupMembershipRepository.findById_MemberId("h1")).thenReturn(java.util.List.of());
        when(askRepository.findByStatus("pending")).thenReturn(java.util.List.of());
        when(boardTaskRepository.findByAssigneeMemberId("h1")).thenReturn(java.util.List.of());
        when(patRepository.findByMemberId("h1")).thenReturn(java.util.List.of());
        Human admin = new Human();
        admin.setId("admin1");
        when(memberService.findAdmins()).thenReturn(java.util.List.of(admin));

        var result = walkService.walkOffboard("h1", null, "admin1");

        assertNotNull(human.getDeactivatedAt());
        verify(memberService).saveHuman(human);
        verify(auditService).log(eq("admin1"), eq("OFFBOARD_WALK"), eq("human"), eq("h1"), any());
    }

    @Test
    void walkOffboard_throwsWhenNoSuccessorAndNoAdmin() {
        Human human = new Human();
        human.setId("h1");
        human.setRbac("member");
        when(memberService.findHuman("h1")).thenReturn(Optional.of(human));
        when(memberService.findAdmins()).thenReturn(java.util.List.of());

        // No admin found and no successor provided — should throw
        assertThrows(IllegalStateException.class, () -> {
            walkService.walkOffboard("h1", null, "admin1");
        });
    }

    @Test
    void walkOffboard_transfersDomainsToSuccessor() {
        Human human = new Human();
        human.setId("h1");
        human.setRbac("member");
        when(memberService.findHuman("h1")).thenReturn(Optional.of(human));
        when(memberService.saveHuman(any())).thenReturn(human);

        DnaDomain domain = new DnaDomain();
        domain.setId("d1");
        domain.setOwnerHumanId("h1");
        when(domainService.findAllIncludingArchived()).thenReturn(java.util.List.of(domain));
        when(domainService.updateOwner("d1", "h2", "admin1")).thenReturn(domain);

        when(agentService.findByOwner("h1")).thenReturn(java.util.List.of());
        when(initiativeService.findAllActive()).thenReturn(java.util.List.of());
        when(goalService.findAllActiveWindowed(any())).thenReturn(java.util.List.of());
        when(memberService.findAllActiveHumans()).thenReturn(java.util.List.of());
        when(proposalService.findAllOpen()).thenReturn(java.util.List.of());
        when(groupMembershipRepository.findById_MemberId("h1")).thenReturn(java.util.List.of());
        when(askRepository.findByStatus("pending")).thenReturn(java.util.List.of());
        when(boardTaskRepository.findByAssigneeMemberId("h1")).thenReturn(java.util.List.of());
        when(patRepository.findByMemberId("h1")).thenReturn(java.util.List.of());

        var result = walkService.walkOffboard("h1", "h2", "admin1");

        assertEquals(1, result.get("domainsTransferred"));
        verify(domainService).updateOwner("d1", "h2", "admin1");
    }

    @Test
    void walkOffboard_transfersAgentsToSuccessor() {
        Human human = new Human();
        human.setId("h1");
        human.setRbac("member");
        when(memberService.findHuman("h1")).thenReturn(Optional.of(human));
        when(memberService.saveHuman(any())).thenReturn(human);

        Agent agent = new Agent();
        agent.setId("a1");
        agent.setOwnerHumanId("h1");
        agent.setTemplateId(null);
        when(agentService.findByOwner("h1")).thenReturn(java.util.List.of(agent));
        when(agentRepository.save(any())).thenReturn(agent);

        when(domainService.findAllIncludingArchived()).thenReturn(java.util.List.of());
        when(initiativeService.findAllActive()).thenReturn(java.util.List.of());
        when(goalService.findAllActiveWindowed(any())).thenReturn(java.util.List.of());
        when(memberService.findAllActiveHumans()).thenReturn(java.util.List.of());
        when(proposalService.findAllOpen()).thenReturn(java.util.List.of());
        when(groupMembershipRepository.findById_MemberId("h1")).thenReturn(java.util.List.of());
        when(askRepository.findByStatus("pending")).thenReturn(java.util.List.of());
        when(boardTaskRepository.findByAssigneeMemberId("h1")).thenReturn(java.util.List.of());
        when(patRepository.findByMemberId("h1")).thenReturn(java.util.List.of());

        var result = walkService.walkOffboard("h1", "h2", "admin1");

        assertEquals(1, result.get("agentsReowned"));
        assertEquals("h2", agent.getOwnerHumanId());
        verify(agentRepository).save(agent);
    }

    @Test
    void walkOffboard_reassignsInitiativeSponsorAndLead() {
        Human human = new Human();
        human.setId("h1");
        human.setRbac("member");
        when(memberService.findHuman("h1")).thenReturn(Optional.of(human));
        when(memberService.saveHuman(any())).thenReturn(human);

        Initiative init = new Initiative();
        init.setId("i1");
        init.setSponsor("h1");
        init.setLead("h1");
        when(initiativeService.findAllActive()).thenReturn(java.util.List.of(init));
        when(initiativeRepository.save(any())).thenReturn(init);

        when(domainService.findAllIncludingArchived()).thenReturn(java.util.List.of());
        when(agentService.findByOwner("h1")).thenReturn(java.util.List.of());
        when(goalService.findAllActiveWindowed(any())).thenReturn(java.util.List.of());
        when(memberService.findAllActiveHumans()).thenReturn(java.util.List.of());
        when(proposalService.findAllOpen()).thenReturn(java.util.List.of());
        when(groupMembershipRepository.findById_MemberId("h1")).thenReturn(java.util.List.of());
        when(askRepository.findByStatus("pending")).thenReturn(java.util.List.of());
        when(boardTaskRepository.findByAssigneeMemberId("h1")).thenReturn(java.util.List.of());
        when(patRepository.findByMemberId("h1")).thenReturn(java.util.List.of());

        var result = walkService.walkOffboard("h1", "h2", "admin1");

        assertEquals("h2", init.getSponsor());
        assertEquals("h2", init.getLead());
        verify(initiativeRepository).save(init);
    }

    @Test
    void walkOffboard_transfersProposals() {
        Human human = new Human();
        human.setId("h1");
        human.setRbac("member");
        when(memberService.findHuman("h1")).thenReturn(Optional.of(human));
        when(memberService.saveHuman(any())).thenReturn(human);

        DnaProposal prop = new DnaProposal();
        prop.setId("p1");
        prop.setProposedBy("h1");
        prop.setStatus("open");
        when(proposalService.findAllOpen()).thenReturn(java.util.List.of(prop));
        when(proposalRepository.save(any())).thenReturn(prop);

        when(domainService.findAllIncludingArchived()).thenReturn(java.util.List.of());
        when(agentService.findByOwner("h1")).thenReturn(java.util.List.of());
        when(initiativeService.findAllActive()).thenReturn(java.util.List.of());
        when(goalService.findAllActiveWindowed(any())).thenReturn(java.util.List.of());
        when(memberService.findAllActiveHumans()).thenReturn(java.util.List.of());
        when(groupMembershipRepository.findById_MemberId("h1")).thenReturn(java.util.List.of());
        when(askRepository.findByStatus("pending")).thenReturn(java.util.List.of());
        when(boardTaskRepository.findByAssigneeMemberId("h1")).thenReturn(java.util.List.of());
        when(patRepository.findByMemberId("h1")).thenReturn(java.util.List.of());

        var result = walkService.walkOffboard("h1", "h2", "admin1");

        assertEquals("h2", prop.getProposedBy());
        assertEquals(1, result.get("proposalsTransferred"));
    }

    @Test
    void walkOffboard_revokesPats() {
        Human human = new Human();
        human.setId("h1");
        human.setRbac("member");
        when(memberService.findHuman("h1")).thenReturn(Optional.of(human));
        when(memberService.saveHuman(any())).thenReturn(human);

        Pat pat = new Pat();
        pat.setId("pat-1");
        pat.setMemberId("h1");
        pat.setRevokedAt(null);
        when(patRepository.findByMemberId("h1")).thenReturn(java.util.List.of(pat));
        when(patRepository.save(any())).thenReturn(pat);

        when(domainService.findAllIncludingArchived()).thenReturn(java.util.List.of());
        when(agentService.findByOwner("h1")).thenReturn(java.util.List.of());
        when(initiativeService.findAllActive()).thenReturn(java.util.List.of());
        when(goalService.findAllActiveWindowed(any())).thenReturn(java.util.List.of());
        when(memberService.findAllActiveHumans()).thenReturn(java.util.List.of());
        when(proposalService.findAllOpen()).thenReturn(java.util.List.of());
        when(groupMembershipRepository.findById_MemberId("h1")).thenReturn(java.util.List.of());
        when(askRepository.findByStatus("pending")).thenReturn(java.util.List.of());
        when(boardTaskRepository.findByAssigneeMemberId("h1")).thenReturn(java.util.List.of());

        var result = walkService.walkOffboard("h1", "h2", "admin1");

        assertNotNull(pat.getRevokedAt());
        assertEquals(1, result.get("patsRevoked"));
    }
}
