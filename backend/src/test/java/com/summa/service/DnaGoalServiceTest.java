package com.summa.service;

import com.summa.repository.DnaGoalRepository;
import com.summa.repository.DnaDomainRepository;
import com.summa.model.DnaGoal;
import com.summa.model.Human;
import com.summa.model.Agent;
import com.summa.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DnaGoalServiceTest {

    @Mock
    private DnaGoalRepository goalRepository;

    @Mock
    private DnaDomainRepository domainRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SecretsScanner secretsScanner;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private DnaGoalService goalService;

    @Test
    void create_setsDefaults() {
        when(domainRepository.findById("domain-1")).thenReturn(java.util.Optional.of(new com.summa.model.DnaDomain()));
        when(goalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doReturn(List.of()).when(secretsScanner).scan(anyString());
        Human human = new Human();
        human.setId("human-1");
        when(memberService.findHuman("h:human-1")).thenReturn(Optional.of(human));

        DnaGoal result = goalService.create("goal-1", "domain-1", "Q1", "Statement", "h:human-1", null, null, null, "admin");

        assertNotNull(result);
        assertEquals("goal-1", result.getId());
        assertEquals("domain-1", result.getDomainId());
        assertEquals("active", result.getStatus());
        assertEquals("linked", result.getInject());
    }

    @Test
    void create_validatesDomainExists() {
        when(domainRepository.findById("domain-1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            goalService.create("goal-1", "domain-1", "Q1", "stmt", "h:human-1", null, null, null, "admin")
        );
    }

    @Test
    void create_skipsDomainValidationWhenBlank() {
        when(goalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doReturn(List.of()).when(secretsScanner).scan(anyString());
        Human human = new Human();
        human.setId("human-1");
        when(memberService.findHuman("h:human-1")).thenReturn(Optional.of(human));

        DnaGoal result = goalService.create("goal-1", "", "Q1", "stmt", "h:human-1", null, null, null, "admin");

        assertNotNull(result);
    }

    @Test
    void create_validatesOwnerExists() {
        when(memberService.findHuman("h:human-1")).thenReturn(Optional.empty());
        when(memberService.findAgent("h:human-1")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            goalService.create("goal-1", null, "Q1", "stmt", "h:human-1", null, null, null, "admin")
        );
    }

    @Test
    void create_succeedsWhenOwnerIsHuman() {
        Human human = new Human();
        human.setId("human-1");
        when(memberService.findHuman("h:human-1")).thenReturn(Optional.of(human));
        when(goalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doReturn(List.of()).when(secretsScanner).scan(anyString());

        DnaGoal result = goalService.create("goal-1", null, "Q1", "stmt", "h:human-1", null, null, null, "admin");

        assertEquals("human-1", result.getOwner());
    }

    @Test
    void create_succeedsWhenOwnerIsAgent() {
        Agent agent = new Agent();
        agent.setId("agent-1");
        when(memberService.findAgent("a:agent-1")).thenReturn(Optional.of(agent));
        when(goalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doReturn(List.of()).when(secretsScanner).scan(anyString());

        DnaGoal result = goalService.create("goal-1", null, "Q1", "stmt", "a:agent-1", null, null, null, "admin");

        assertEquals("agent-1", result.getOwner());
    }

    @Test
    void updateStatus_allowsActiveToMet() {
        DnaGoal goal = new DnaGoal();
        goal.setId("goal-1");
        goal.setStatus("active");
        when(goalRepository.findById("goal-1")).thenReturn(Optional.of(goal));
        when(goalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaGoal result = goalService.updateStatus("goal-1", "met", "admin");

        assertEquals("met", result.getStatus());
    }

    @Test
    void updateStatus_rejectsTerminalStatusTransition() {
        DnaGoal goal = new DnaGoal();
        goal.setId("goal-1");
        goal.setStatus("met");
        when(goalRepository.findById("goal-1")).thenReturn(Optional.of(goal));

        assertThrows(IllegalArgumentException.class, () ->
            goalService.updateStatus("goal-1", "active", "admin")
        );
    }

    @Test
    void updateStatus_rejectsInvalidStatus() {
        DnaGoal goal = new DnaGoal();
        goal.setId("goal-1");
        goal.setStatus("active");
        when(goalRepository.findById("goal-1")).thenReturn(Optional.of(goal));

        assertThrows(IllegalArgumentException.class, () ->
            goalService.updateStatus("goal-1", "invalid", "admin")
        );
    }

    @Test
    void updateStatus_throwsWhenNotFound() {
        when(goalRepository.findById("goal-1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            goalService.updateStatus("goal-1", "met", "admin")
        );
    }

    @Test
    void updateWindow_allowsPartialUpdate() {
        Instant now = Instant.now();
        DnaGoal goal = new DnaGoal();
        goal.setId("goal-1");
        goal.setStatus("active");
        goal.setEffectiveFrom(now.minusSeconds(86400));
        when(goalRepository.findById("goal-1")).thenReturn(Optional.of(goal));
        when(goalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaGoal result = goalService.updateWindow("goal-1", null, now.plusSeconds(86400), "admin");

        // effectiveFrom is preserved since null was not provided
        assertNotNull(result.getEffectiveFrom());
        assertNotNull(result.getEffectiveTo());
    }

    @Test
    void updateWindow_rejectsBothNull() {
        DnaGoal goal = new DnaGoal();
        goal.setId("goal-1");
        goal.setStatus("active");
        when(goalRepository.findById("goal-1")).thenReturn(Optional.of(goal));

        assertThrows(IllegalArgumentException.class, () ->
            goalService.updateWindow("goal-1", null, null, "admin")
        );
    }

    @Test
    void updateWindow_rejectsToBeforeFrom() {
        DnaGoal goal = new DnaGoal();
        goal.setId("goal-1");
        goal.setStatus("active");
        when(goalRepository.findById("goal-1")).thenReturn(Optional.of(goal));

        Instant from = Instant.now();
        Instant to = Instant.now().minusSeconds(86400);
        assertThrows(IllegalArgumentException.class, () ->
            goalService.updateWindow("goal-1", from, to, "admin")
        );
    }

    @Test
    void updateWindow_rejectsTerminalGoal() {
        DnaGoal goal = new DnaGoal();
        goal.setId("goal-1");
        goal.setStatus("retired");
        when(goalRepository.findById("goal-1")).thenReturn(Optional.of(goal));

        assertThrows(IllegalArgumentException.class, () ->
            goalService.updateWindow("goal-1", Instant.now(), Instant.now().plusSeconds(86400), "admin")
        );
    }

    @Test
    void findActiveInject_returnsMatchingGoals() {
        Instant now = Instant.now();
        when(goalRepository.findActiveInject("linked", now)).thenReturn(List.of());

        List<DnaGoal> results = goalService.findActiveInject("linked", now);

        assertNotNull(results);
        verify(goalRepository).findActiveInject("linked", now);
    }

    @Test
    void findAllActiveWindowed_returnsWindowedGoals() {
        Instant now = Instant.now();
        when(goalRepository.findAllActiveWindowed(now)).thenReturn(List.of());

        List<DnaGoal> results = goalService.findAllActiveWindowed(now);

        assertNotNull(results);
        verify(goalRepository).findAllActiveWindowed(now);
    }
}
