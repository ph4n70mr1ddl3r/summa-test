package com.summa.service;

import com.summa.repository.InitiativeRepository;
import com.summa.model.Initiative;
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
class InitiativeServiceTest {

    @Mock
    private InitiativeRepository initiativeRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private AskService askService;

    @InjectMocks
    private InitiativeService initiativeService;

    @Test
    void create_initiativeWithDefaults() {
        Initiative init = new Initiative();
        init.setId("i1");
        init.setTitle("Launch product");
        init.setStatus("proposed");
        when(initiativeRepository.save(any())).thenReturn(init);

        Initiative result = initiativeService.create("i1", "Launch product", "h1", "h1", null, null,
            Instant.now().plusSeconds(86400), "[]");

        assertNotNull(result);
        assertEquals("proposed", result.getStatus());
    }

    @Test
    void activate_changesStatusToActive() {
        Initiative init = new Initiative();
        init.setId("i1");
        init.setStatus("proposed");
        when(initiativeRepository.findById("i1")).thenReturn(Optional.of(init));
        when(initiativeRepository.save(any())).thenReturn(init);

        Initiative result = initiativeService.activate("i1", "admin");

        assertEquals("active", result.getStatus());
        verify(auditService).log(eq("admin"), eq("ACTIVATE"), eq("initiative"), eq("i1"), isNull());
    }

    @Test
    void activate_throwsWhenNotFound() {
        when(initiativeRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            initiativeService.activate("missing", "admin");
        });
    }

    @Test
    void close_changesStatusToClosed() {
        Initiative init = new Initiative();
        init.setId("i1");
        init.setStatus("active");
        init.setClosedAt(null);
        when(initiativeRepository.findById("i1")).thenReturn(Optional.of(init));
        when(initiativeRepository.save(any())).thenReturn(init);

        Initiative result = initiativeService.close("i1", "admin");

        assertEquals("closed", result.getStatus());
        assertNotNull(result.getClosedAt());
        verify(auditService).log(eq("admin"), eq("CLOSE"), eq("initiative"), eq("i1"), isNull());
    }

    @Test
    void pause_changesStatusToPaused() {
        Initiative init = new Initiative();
        init.setId("i1");
        init.setStatus("active");
        when(initiativeRepository.findById("i1")).thenReturn(Optional.of(init));
        when(initiativeRepository.save(any())).thenReturn(init);

        Initiative result = initiativeService.pause("i1", "admin");

        assertEquals("paused", result.getStatus());
    }

    @Test
    void resume_changesStatusToActive() {
        Initiative init = new Initiative();
        init.setId("i1");
        init.setStatus("paused");
        when(initiativeRepository.findById("i1")).thenReturn(Optional.of(init));
        when(initiativeRepository.save(any())).thenReturn(init);

        Initiative result = initiativeService.resume("i1", "admin");

        assertEquals("active", result.getStatus());
    }

    @Test
    void findByStatus_filtersByStatus() {
        Initiative i1 = new Initiative();
        i1.setId("i1");
        i1.setStatus("active");
        Initiative i2 = new Initiative();
        i2.setId("i2");
        i2.setStatus("closed");
        when(initiativeRepository.findByStatus("active")).thenReturn(java.util.List.of(i1));

        var results = initiativeService.findByStatus("active");
        assertEquals(1, results.size());
        assertEquals("i1", results.get(0).getId());
    }
}
