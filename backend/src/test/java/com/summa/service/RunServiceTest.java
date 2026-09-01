package com.summa.service;

import com.summa.repository.RunRepository;
import com.summa.repository.InitiativeRepository;
import com.summa.model.Run;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RunServiceTest {

    @Mock
    private RunRepository runRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private InitiativeRepository initiativeRepository;

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

        assertThrows(IllegalArgumentException.class, () -> {
            runService.start("missing");
        });
    }
}
