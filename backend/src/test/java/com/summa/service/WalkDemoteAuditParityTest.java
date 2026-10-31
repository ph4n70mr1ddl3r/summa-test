package com.summa.service;

import com.summa.model.Ask;
import com.summa.model.Human;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.summa.repository.AskRepository;
import com.summa.repository.AgentRepository;
import com.summa.repository.BoardTaskRepository;
import com.summa.repository.DnaGoalRepository;
import com.summa.repository.DnaProposalRepository;
import com.summa.repository.GroupMembershipRepository;
import com.summa.repository.GroupRepository;
import com.summa.repository.InitiativeRepository;
import com.summa.repository.PatRepository;
import com.summa.repository.RoleTemplateRepository;
import com.summa.repository.WorkspaceRepository;
import com.summa.repository.DnaDomainRepository;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WalkDemoteAuditParityTest {

    @Mock private MemberService memberService;
    @Mock private AgentService agentService;
    @Mock private InitiativeService initiativeService;
    @Mock private BoardTaskService boardTaskService;
    @Mock private DnaProposalService proposalService;
    @Mock private AuditService auditService;
    @Mock private AskService askService;
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
    @Mock private GroupRepository groupRepository;
    @Mock private RoleTemplateRepository roleTemplateRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private DnaDomainRepository domainRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OffboardingWalkService walkService;

    @Test
    void walkDemote_reassignmentOfAskToAuditsTheCustodyTransfer() {
        // OFB-031: the demotion walk reassigns asks addressed to the demoted member
        // to admin custody. The offboarding walk names that transfer with an
        // OFFBOARD_REASSIGN_ASK_TO audit row; the demotion walk must name its own
        // with a DEMOTE_REASSIGN_ASK_TO row, so a custody transfer is never an
        // un-audited mutation.
        Human human = new Human();
        human.setId("h1");
        human.setRbac("owner");
        when(memberService.findHuman("h1")).thenReturn(Optional.of(human));
        when(memberService.findAllActiveHumans()).thenReturn(List.of());
        when(domainService.findAllIncludingArchived(anyInt())).thenReturn(List.of());
        when(agentService.findByOwner(eq("h1"), anyInt())).thenReturn(List.of());
        when(proposalService.findAllOpen()).thenReturn(List.of());
        when(initiativeService.findAllActive(anyInt())).thenReturn(List.of());
        when(goalService.findAllActiveWindowed(any())).thenReturn(List.of());
        when(boardTaskRepository.findByAssigneeMemberId(anyString())).thenReturn(List.of());
        when(patRepository.findByMemberId(anyString())).thenReturn(List.of());
        when(groupRepository.findAll()).thenReturn(List.of());
        when(workspaceRepository.findByArchivedAtIsNullOrdered(anyInt())).thenReturn(List.of());

        Human admin = new Human();
        admin.setId("admin1");
        when(memberService.findAdmins()).thenReturn(List.of(admin));

        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setTo("h:h1");
        when(askRepository.findByStatusPendingOrdered(anyInt())).thenReturn(List.of(ask));
        when(askRepository.save(any())).thenReturn(ask);

        var result = walkService.walkDemote("h1", "member", "admin1");

        // The ask is re-pointed to custody...
        assertEquals("h:admin1", ask.getTo());
        assertEquals(1, result.get("asksClosed"));
        // ...and the transfer is named in an audit row, matching the offboarding walk.
        verify(auditService).logSystem(eq("DEMOTE_REASSIGN_ASK_TO"), eq("ask"), eq("ask-1"), anyString());
        verify(askRepository).save(ask);
    }
}
