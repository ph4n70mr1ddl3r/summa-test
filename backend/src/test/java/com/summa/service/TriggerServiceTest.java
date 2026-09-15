package com.summa.service;

import com.summa.repository.TriggerRepository;
import com.summa.repository.TriggerFiringRepository;
import com.summa.model.Trigger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.summa.exception.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class TriggerServiceTest {

    @Mock
    private TriggerRepository triggerRepository;

    @Mock
    private TriggerFiringRepository firingRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private TriggerService triggerService;

    @Test
    void create_withDefaults() {
        Trigger trigger = new Trigger();
        trigger.setId("t1");
        when(triggerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Trigger result = triggerService.create("t1", "schedule", "* * * * *", "agent-1", null, null, null, "actor");

        assertNotNull(result);
        assertEquals("standard", result.getCriticality());
        assertEquals("{}", result.getConfig());
        assertEquals("active", result.getStatus());
    }

    @Test
    void findById_returnsPresent() {
        Trigger t = new Trigger();
        t.setId("t1");
        when(triggerRepository.findById("t1")).thenReturn(Optional.of(t));

        Optional<Trigger> result = triggerService.findById("t1");

        assertTrue(result.isPresent());
    }

    @Test
    void pause_findsAndPauses() {
        Trigger t = new Trigger();
        t.setId("t1");
        t.setStatus("active");
        when(triggerRepository.findById("t1")).thenReturn(Optional.of(t));
        when(triggerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Trigger result = triggerService.pause("t1", "admin");

        assertEquals("paused", result.getStatus());
        verify(auditService).log(eq("admin"), eq("PAUSE_TRIGGER"), eq("trigger"), eq("t1"), isNull());
    }

    @Test
    void pause_throwsWhenNotFound() {
        when(triggerRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            triggerService.pause("missing", "admin"));
    }

    @Test
    void resume_findsAndResumes() {
        Trigger t = new Trigger();
        t.setId("t1");
        t.setStatus("paused");
        when(triggerRepository.findById("t1")).thenReturn(Optional.of(t));
        when(triggerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Trigger result = triggerService.resume("t1", "admin");

        assertEquals("active", result.getStatus());
    }

    @Test
    void resume_throwsWhenNotFound() {
        when(triggerRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            triggerService.resume("missing", "admin"));
    }

    @Test
    void archive_findsAndArchives() {
        Trigger t = new Trigger();
        t.setId("t1");
        t.setStatus("active");
        when(triggerRepository.findById("t1")).thenReturn(Optional.of(t));
        when(triggerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Trigger result = triggerService.archive("t1", "admin");

        assertEquals("archived", result.getStatus());
        verify(auditService).log(eq("admin"), eq("ARCHIVE_TRIGGER"), eq("trigger"), eq("t1"), isNull());
    }

    @Test
    void archive_throwsWhenNotFound() {
        when(triggerRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            triggerService.archive("missing", "admin"));
    }

    @Test
    void getStats_countsByStatus() {
        when(triggerRepository.findByStatus("active")).thenReturn(List.of(new Trigger(), new Trigger()));
        when(triggerRepository.findByStatus("paused")).thenReturn(List.of(new Trigger()));
        when(triggerRepository.findByStatus("archived")).thenReturn(List.of());

        var stats = triggerService.getStats();

        assertEquals(2L, stats.get("active"));
        assertEquals(1L, stats.get("paused"));
        assertEquals(0L, stats.get("archived"));
        assertEquals(3L, stats.get("total"));
    }
}
