package com.summa.service;

import com.summa.repository.TriggerRepository;
import com.summa.repository.TriggerFiringRepository;
import com.summa.model.Trigger;
import com.summa.model.TriggerFiring;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import com.summa.exception.EntityNotFoundException;
import com.summa.util.JsonHelpers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.support.CronExpression;

@Service
public class TriggerService {
    private final TriggerRepository triggerRepository;
    private final TriggerFiringRepository firingRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public TriggerService(TriggerRepository triggerRepository, TriggerFiringRepository firingRepository,
                          AuditService auditService, ObjectMapper objectMapper) {
        this.triggerRepository = triggerRepository;
        this.firingRepository = firingRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Trigger create(String name, String kind, String expression, String agentId,
                           String workspaceId, String criticality, String config, String actor) {
        Trigger trigger = new Trigger();
        trigger.setId(UUID.randomUUID().toString());
        trigger.setName(name);
        trigger.setKind(kind);
        trigger.setExpression(expression);
        trigger.setAgentId(agentId);
        trigger.setWorkspaceId(workspaceId);
        trigger.setCriticality(criticality != null ? criticality : "standard");
        trigger.setConfig(config != null ? config : "{}");
        trigger.setStatus("active");

        Trigger saved = triggerRepository.save(trigger);
        auditService.log(actor, "CREATE_TRIGGER", "trigger", saved.getId(),
            String.format("{\"kind\":%s,\"expression\":%s}", JsonHelpers.jsonString(kind), JsonHelpers.jsonString(expression)));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Trigger> findById(String id) {
        return triggerRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Trigger> findAll() {
        return triggerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Trigger> findByAgent(String agentId) {
        return triggerRepository.findByAgentId(agentId);
    }

    @Transactional
    public Trigger pause(String id, String actor) {
        Trigger trigger = triggerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trigger not found: " + id));
        if (!"active".equals(trigger.getStatus())) {
            throw new IllegalStateException("Only active triggers can be paused");
        }
        trigger.setStatus("paused");
        Trigger saved = triggerRepository.save(trigger);
        auditService.log(actor, "PAUSE_TRIGGER", "trigger", id, null);
        return saved;
    }

    @Transactional
    public Trigger resume(String id, String actor) {
        Trigger trigger = triggerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trigger not found: " + id));
        if (!"paused".equals(trigger.getStatus())) {
            throw new IllegalStateException("Only paused triggers can be resumed");
        }
        trigger.setStatus("active");
        Trigger saved = triggerRepository.save(trigger);
        auditService.log(actor, "RESUME_TRIGGER", "trigger", id, null);
        return saved;
    }

    @Transactional
    public Trigger archive(String id, String actor) {
        Trigger trigger = triggerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trigger not found: " + id));
        if ("archived".equals(trigger.getStatus())) {
            throw new IllegalStateException("Trigger is already archived");
        }
        trigger.setStatus("archived");
        Trigger saved = triggerRepository.save(trigger);
        auditService.log(actor, "ARCHIVE_TRIGGER", "trigger", id, null);
        return saved;
    }

    /**
     * SUB-052: Scheduled check for schedule-based triggers with idempotency.
     * Every firing carries a deterministic key; duplicates within the dedupe window
     * are refused and return the original run.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkScheduledTriggers() {
        List<Trigger> activeTriggers = triggerRepository.findByStatus("active");
        Instant now = Instant.now();

        for (Trigger trigger : activeTriggers) {
            if (!"schedule".equals(trigger.getKind())) continue;
            try {
                checkSingleTrigger(trigger, now);
            } catch (Exception e) {
                auditService.logSystem("TRIGGER_CHECK_FAIL", "trigger", trigger.getId(),
                    JsonHelpers.toJson(Map.of("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), objectMapper));
            }
        }
    }

    private void checkSingleTrigger(Trigger trigger, Instant now) {
        // Parse cron expression using Spring's CronExpression for proper validation
        String expr = trigger.getExpression();
        CronExpression cron;
        try {
            cron = CronExpression.parse(expr);
        } catch (Exception e) {
            trigger.setStatus("error");
            triggerRepository.save(trigger);
            auditService.logSystem("TRIGGER_INVALID_CRON", "trigger", trigger.getId(),
                JsonHelpers.toJson(Map.of("expression", expr != null ? expr : "", "error", e.getMessage() != null ? e.getMessage() : "unknown"), objectMapper));
            return;
        }

        // Check if the cron fires at or before the current minute
        Instant nextFire = cron.next(now);
        if (nextFire == null) {
            auditService.logSystem("TRIGGER_CRON_INVALID", "trigger", trigger.getId(),
                String.format("{\"expression\":\"%s\"}", expr != null ? expr : ""));
            return;
        }
        // Fire if the next scheduled time is within the current minute window
        Instant nowTruncated = now.truncatedTo(ChronoUnit.MINUTES);
        if (nextFire.isBefore(nowTruncated.plusSeconds(60))) {
            // SUB-052: Idempotency key = trigger_id + scheduled_time
            String idempotencyKey = trigger.getId() + ":" + nowTruncated;
            Optional<TriggerFiring> existing = firingRepository
                    .findByTriggerIdAndIdempotencyKey(trigger.getId(), idempotencyKey);
            if (existing.isPresent()) {
                // Already fired — return original run (SUB-052 replay)
                auditService.logSystem("REPLAY_FIRING", "trigger_firing", existing.get().getId(), null);
                return;
            }

            // Record firing
            TriggerFiring firing = new TriggerFiring();
            firing.setId(UUID.randomUUID().toString());
            firing.setTriggerId(trigger.getId());
            firing.setIdempotencyKey(idempotencyKey);
            firing.setFiredAt(now);
            firingRepository.save(firing);

            trigger.setLastFiredAt(now);
            triggerRepository.save(trigger);
            auditService.logSystem("FIRE_TRIGGER", "trigger", trigger.getId(), null);
        }
    }

    public Map<String, Object> getStats() {
        long active = triggerRepository.findByStatus("active").size();
        long paused = triggerRepository.findByStatus("paused").size();
        long archived = triggerRepository.findByStatus("archived").size();
        return Map.of("active", active, "paused", paused, "archived", archived, "total", active + paused + archived);
    }
}
