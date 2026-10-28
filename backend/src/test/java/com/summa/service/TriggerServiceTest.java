package com.summa.service;

import com.summa.repository.TriggerRepository;
import com.summa.repository.TriggerFiringRepository;
import com.summa.model.Trigger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import com.summa.model.Run;
import com.summa.model.TriggerFiring;

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

    @Mock
    private RunService runService;

    @org.mockito.Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TriggerService triggerService;

    @Test
    void create_withDefaults() {
        Trigger trigger = new Trigger();
        trigger.setId("t1");
        when(triggerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Trigger result = triggerService.create("t1", "schedule", "0 * * * * *", "agent-1", null, null, null, "actor");

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

    @Test
    void create_apiTriggerDoesNotParseItsEventNameAsCron() {
        when(triggerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Trigger result = triggerService.create("webhook", "api", "event-order-created", "agent-1",
                null, "standard", null, "actor");

        assertEquals("api", result.getKind());
        assertEquals("event-order-created", result.getExpression());
    }

    @Test
    void create_eventTriggerDoesNotParseItsSourceIdAsCron() {
        when(triggerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Trigger result = triggerService.create("event", "event", "order.created", "agent-1",
                null, "standard", null, "actor");

        assertEquals("event", result.getKind());
        assertEquals("order.created", result.getExpression());
    }

    @Test
    void checkScheduledTriggers_firesADueScheduleAndQueuesARun() {
        Trigger trigger = new Trigger();
        trigger.setId("t1");
        trigger.setName("Nightly digest");
        trigger.setKind("schedule");
        trigger.setExpression("* * * * * *");
        trigger.setAgentId("agent-1");
        trigger.setWorkspaceId("ws-1");
        trigger.setStatus("active");

        Run run = new Run();
        run.setId("run-1");
        when(triggerRepository.findByStatus("active")).thenReturn(List.of(trigger));
        when(triggerRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(firingRepository.findByTriggerIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(runService.create(eq("agent-1"), eq("ws-1"), isNull(), eq("t1"), anyString(), eq("system")))
            .thenReturn(run);

        triggerService.checkScheduledTriggers();

        verify(runService).create(eq("agent-1"), eq("ws-1"), isNull(), eq("t1"), anyString(), eq("system"));
        ArgumentCaptor<TriggerFiring> firing = ArgumentCaptor.forClass(TriggerFiring.class);
        verify(firingRepository).save(firing.capture());
        assertEquals("run-1", firing.getValue().getRunId());
        assertNotNull(trigger.getLastFiredAt());
        verify(auditService).logSystem(eq("FIRE_TRIGGER"), eq("trigger"), eq("t1"), contains("run-1"));
    }

    @Test
    void checkScheduledTriggers_replaysTheOriginalFiringWithoutAnotherRun() {
        Trigger trigger = new Trigger();
        trigger.setId("t1");
        trigger.setName("Digest");
        trigger.setKind("schedule");
        trigger.setExpression("* * * * * *");
        trigger.setAgentId("agent-1");
        trigger.setStatus("active");

        TriggerFiring existing = new TriggerFiring();
        existing.setId("firing-1");
        when(triggerRepository.findByStatus("active")).thenReturn(List.of(trigger));
        when(firingRepository.findByTriggerIdAndIdempotencyKey(any(), any())).thenReturn(Optional.of(existing));

        triggerService.checkScheduledTriggers();

        verify(runService, never()).create(any(), any(), any(), any(), any(), any());
        verify(auditService).logSystem(eq("REPLAY_FIRING"), eq("trigger_firing"), eq("firing-1"), isNull());
    }

    @Test
    void checkScheduledTriggers_firesACatchUpAfterDowntimeInsteadOfSkipping() {
        Trigger trigger = new Trigger();
        trigger.setId("t1");
        trigger.setName("Digest");
        trigger.setKind("schedule");
        trigger.setExpression("0 0 * * * *");
        trigger.setAgentId("agent-1");
        trigger.setStatus("active");
        trigger.setLastFiredAt(Instant.now().minusSeconds(3 * 3600));

        Run run = new Run();
        run.setId("run-catchup");
        when(triggerRepository.findByStatus("active")).thenReturn(List.of(trigger));
        when(triggerRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(firingRepository.findByTriggerIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(runService.create(any(), any(), any(), any(), any(), any())).thenReturn(run);

        triggerService.checkScheduledTriggers();

        verify(runService, times(1)).create(any(), any(), any(), any(), any(), any());
        assertTrue(trigger.getLastFiredAt().isAfter(Instant.now().minusSeconds(5)));
    }

    @Test
    void checkScheduledTriggers_doesNotFireBeforeTheNextOccurrence() {
        Trigger trigger = new Trigger();
        trigger.setId("t1");
        trigger.setName("Daily");
        trigger.setKind("schedule");
        trigger.setExpression("0 0 0 1 1 *");
        trigger.setAgentId("agent-1");
        trigger.setStatus("active");
        trigger.setLastFiredAt(Instant.now());

        when(triggerRepository.findByStatus("active")).thenReturn(List.of(trigger));

        triggerService.checkScheduledTriggers();

        verify(runService, never()).create(any(), any(), any(), any(), any(), any());
    }
}
