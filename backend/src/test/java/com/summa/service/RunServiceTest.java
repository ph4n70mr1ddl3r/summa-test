package com.summa.service;

import com.summa.repository.RunRepository;
import com.summa.repository.InitiativeRepository;
import com.summa.repository.AgentRepository;
import com.summa.model.Run;
import com.summa.model.Initiative;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.summa.exception.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class RunServiceTest {

    @Mock
    private RunRepository runRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private InitiativeRepository initiativeRepository;

    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private RunService runService;

    @Test
    void startUpdatesStatus() {
        Run run = new Run();
        run.setId("run-1");
        run.setStatus("queued");
        when(runRepository.findById("run-1")).thenReturn(Optional.of(run));
        when(runRepository.save(any())).thenReturn(run);

        Run result = runService.start("run-1");

        assertEquals("running", result.getStatus());
        assertNotNull(result.getStartedAt());
    }

    @Test
    void completeSetsResult() {
        Run run = new Run();
        run.setId("run-1");
        run.setStatus("running");
        when(runRepository.findById("run-1")).thenReturn(Optional.of(run));
        when(runRepository.save(any())).thenReturn(run);

        Run result = runService.complete("run-1", "success", 100L, 0.01);

        assertEquals("completed", result.getStatus());
        assertEquals("success", result.getResult());
    }

    @Test
    void failSetsError() {
        Run run = new Run();
        run.setId("run-1");
        run.setStatus("running");
        when(runRepository.findById("run-1")).thenReturn(Optional.of(run));
        when(runRepository.save(any())).thenReturn(run);

        Run result = runService.fail("run-1", "Something went wrong");

        assertEquals("failed", result.getStatus());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void start_throwsWhenNotFound() {
        when(runRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            runService.start("missing");
        });
    }

    @Test
    void start_throwsWhenNotQueued() {
        Run run = new Run();
        run.setId("run-1");
        run.setStatus("running");
        when(runRepository.findById("run-1")).thenReturn(Optional.of(run));

        assertThrows(IllegalStateException.class, () -> {
            runService.start("run-1");
        });
    }

    @Test
    void complete_throwsWhenNotRunning() {
        Run run = new Run();
        run.setId("run-1");
        run.setStatus("queued");
        when(runRepository.findById("run-1")).thenReturn(Optional.of(run));

        assertThrows(IllegalStateException.class, () -> {
            runService.complete("run-1", "result", null, null);
        });
    }

    @Test
    void fail_throwsWhenNotRunning() {
        Run run = new Run();
        run.setId("run-1");
        run.setStatus("queued");
        when(runRepository.findById("run-1")).thenReturn(Optional.of(run));

        assertThrows(IllegalStateException.class, () -> {
            runService.fail("run-1", "error");
        });
    }

    @Test
    void cancel_throwsWhenNotFound() {
        when(runRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            runService.cancel("missing");
        });
    }

    @Test
    void cancel_allowsQueued() {
        Run run = new Run();
        run.setId("run-1");
        run.setStatus("queued");
        when(runRepository.findById("run-1")).thenReturn(Optional.of(run));
        when(runRepository.save(any())).thenReturn(run);

        Run result = runService.cancel("run-1");

        assertEquals("cancelled", result.getStatus());
    }

    @Test
    void cancel_allowsRunning() {
        Run run = new Run();
        run.setId("run-1");
        run.setStatus("running");
        when(runRepository.findById("run-1")).thenReturn(Optional.of(run));
        when(runRepository.save(any())).thenReturn(run);

        Run result = runService.cancel("run-1");

        assertEquals("cancelled", result.getStatus());
    }

    @Test
    void cancel_throwsWhenAlreadyCompleted() {
        Run run = new Run();
        run.setId("run-1");
        run.setStatus("completed");
        when(runRepository.findById("run-1")).thenReturn(Optional.of(run));

        assertThrows(IllegalStateException.class, () -> {
            runService.cancel("run-1");
        });
    }

    @Test
    void suspend_throwsWhenNotRunning() {
        Run run = new Run();
        run.setId("run-1");
        run.setStatus("queued");
        when(runRepository.findById("run-1")).thenReturn(Optional.of(run));

        assertThrows(IllegalStateException.class, () -> {
            runService.suspend("run-1");
        });
    }

    @Test
    void resume_throwsWhenNotSuspended() {
        Run run = new Run();
        run.setId("run-1");
        run.setStatus("running");
        when(runRepository.findById("run-1")).thenReturn(Optional.of(run));

        assertThrows(IllegalStateException.class, () -> {
            runService.resume("run-1");
        });
    }

    @Test
    void create_validatesActiveInitiative() {
        Initiative init = new Initiative();
        init.setId("init-1");
        init.setStatus("inactive");
        when(initiativeRepository.findById("init-1")).thenReturn(Optional.of(init));

        assertThrows(IllegalStateException.class, () -> {
            runService.create("agent-1", "ws-1", "init-1", null, "prompt", "actor");
        });
    }

    @Test
    void create_allowsNullInitiative() {
        Run run = new Run();
        run.setId("run-1");
        run.setStatus("queued");
        when(runRepository.save(any())).thenReturn(run);

        Run result = runService.create("agent-1", "ws-1", null, null, "prompt", "actor");

        assertEquals("queued", result.getStatus());
        assertNull(result.getInitiativeId());
    }

    @Test
    void create_throwsWhenInitiativeNotFound() {
        when(initiativeRepository.findById("init-99")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            runService.create("agent-1", "ws-1", "init-99", null, "prompt", "actor");
        });
    }

    @Test
    void findByStatus_returnsList() {
        Run run = new Run();
        run.setId("run-1");
        when(runRepository.findByStatusOrderByCreatedAtDesc("running", 10)).thenReturn(List.of(run));

        List<Run> result = runService.findByStatus("running", 10);

        assertEquals(1, result.size());
    }

    @Test
    void countByStatus_returnsCount() {
        when(runRepository.countByStatus("running")).thenReturn(5L);

        long count = runService.countByStatus("running");

        assertEquals(5, count);
    }

    @Test
    void agentExists_returnsTrue() {
        when(agentRepository.existsById("agent-1")).thenReturn(true);

        assertTrue(runService.agentExists("agent-1"));
    }

    @Test
    void agentExists_returnsFalse() {
        when(agentRepository.existsById("agent-99")).thenReturn(false);

        assertFalse(runService.agentExists("agent-99"));
    }
}
