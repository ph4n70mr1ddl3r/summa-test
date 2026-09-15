package com.summa.service;

import com.summa.repository.DnaGoalRepository;
import com.summa.model.DnaGoal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.summa.exception.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class DnaGoalServiceTest {

    @Mock
    private DnaGoalRepository goalRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private DnaGoalService goalService;

    @Test
    void create_withDefaults() {
        DnaGoal goal = new DnaGoal();
        goal.setId("g1");
        when(goalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DnaGoal result = goalService.create("g1", "d1", "Q1", "statement", "h:owner-1", null,
            Instant.now(), null, "actor");

        assertNotNull(result);
        assertEquals("linked", result.getInject());
        assertEquals("active", result.getStatus());
    }

    @Test
    void create_throwsWhenNullOwner() {
        assertThrows(IllegalArgumentException.class, () ->
            goalService.create("g1", "d1", "Q1", "stmt", null, null, Instant.now(), null, "actor"));
    }

    @Test
    void create_throwsWhenBlankOwner() {
        assertThrows(IllegalArgumentException.class, () ->
            goalService.create("g1", "d1", "Q1", "stmt", "   ", null, Instant.now(), null, "actor"));
    }

    @Test
    void create_throwsWhenInvalidKeyedUnion() {
        assertThrows(IllegalArgumentException.class, () ->
            goalService.create("g1", "d1", "Q1", "stmt", "bad format", null, Instant.now(), null, "actor"));
    }

    @Test
    void updateStatus_validTransition() {
        DnaGoal goal = new DnaGoal();
        goal.setId("g1");
        goal.setStatus("active");
        when(goalRepository.findById("g1")).thenReturn(Optional.of(goal));
        when(goalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DnaGoal result = goalService.updateStatus("g1", "met", "admin");

        assertEquals("met", result.getStatus());
    }

    @Test
    void updateStatus_throwsForTerminalGoal() {
        DnaGoal goal = new DnaGoal();
        goal.setId("g1");
        goal.setStatus("met");
        when(goalRepository.findById("g1")).thenReturn(Optional.of(goal));

        assertThrows(IllegalStateException.class, () ->
            goalService.updateStatus("g1", "active", "admin"));
    }

    @Test
    void updateStatus_throwsForInvalidStatus() {
        DnaGoal goal = new DnaGoal();
        goal.setId("g1");
        goal.setStatus("active");
        when(goalRepository.findById("g1")).thenReturn(Optional.of(goal));

        assertThrows(IllegalArgumentException.class, () ->
            goalService.updateStatus("g1", "invalid", "admin"));
    }

    @Test
    void updateStatus_throwsWhenNotFound() {
        when(goalRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            goalService.updateStatus("missing", "met", "admin"));
    }

    @Test
    void updateWindow_providesFromOnly() {
        DnaGoal goal = new DnaGoal();
        goal.setId("g1");
        when(goalRepository.findById("g1")).thenReturn(Optional.of(goal));
        when(goalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Instant newFrom = Instant.now().plusSeconds(86400);
        DnaGoal result = goalService.updateWindow("g1", newFrom, null, "admin");

        assertEquals(newFrom, result.getEffectiveFrom());
    }

    @Test
    void updateWindow_throwsWhenBothNull() {
        DnaGoal goal = new DnaGoal();
        goal.setId("g1");
        when(goalRepository.findById("g1")).thenReturn(Optional.of(goal));

        assertThrows(IllegalArgumentException.class, () ->
            goalService.updateWindow("g1", null, null, "admin"));
    }
}
