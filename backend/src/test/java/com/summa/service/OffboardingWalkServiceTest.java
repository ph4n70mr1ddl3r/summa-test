package com.summa.service;

import com.summa.repository.GroupMembershipRepository;
import com.summa.model.Human;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @InjectMocks
    private OffboardingWalkService walkService;

    @Test
    void walkOffboard_refusesLastAdmin() {
        when(memberService.countActiveAdmins()).thenReturn(1L);
        Human human = new Human();
        human.setId("h1");
        human.setRbac("admin");
        when(memberService.findHuman("h1")).thenReturn(Optional.of(human));

        assertThrows(IllegalStateException.class, () -> {
            walkService.walkOffboard("h1", null, "admin2");
        });
    }

    @Test
    void walkOffboard_deactivatesHuman() {
        when(memberService.countActiveAdmins()).thenReturn(2L);
        Human human = new Human();
        human.setId("h1");
        human.setRbac("member");
        human.setDeactivatedAt(null);
        when(memberService.findHuman("h1")).thenReturn(Optional.of(human));
        when(memberService.saveHuman(any())).thenReturn(human);
        when(memberService.findAdmins()).thenReturn(java.util.List.of());
        when(domainService.findAll()).thenReturn(java.util.List.of());
        when(agentService.findByOwner("h1")).thenReturn(java.util.List.of());
        when(initiativeService.findAll()).thenReturn(java.util.List.of());
        when(goalService.findAllActiveWindowed(any())).thenReturn(java.util.List.of());
        when(memberService.findAllActiveHumans()).thenReturn(java.util.List.of());
        when(proposalService.findAllOpen()).thenReturn(java.util.List.of());
        when(groupMembershipRepository.findById_MemberId("h1")).thenReturn(java.util.List.of());

        var result = walkService.walkOffboard("h1", null, "admin1");

        assertNotNull(human.getDeactivatedAt());
        verify(memberService).saveHuman(human);
        verify(auditService).log(eq("admin1"), eq("OFFBOARD_WALK"), eq("human"), eq("h1"), any());
    }
}
