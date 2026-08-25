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
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TriggerService {
    private final TriggerRepository triggerRepository;
    private final TriggerFiringRepository firingRepository;
    private final AuditService auditService;
    private final Map<String, Instant> lastFireTimes = new ConcurrentHashMap<>();

    public TriggerService(TriggerRepository triggerRepository, TriggerFiringRepository firingRepository,
                           AuditService auditService) {
        this.triggerRepository = triggerRepository;
        this.firingRepository = firingRepository;
        this.auditService = auditService;
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
            String.format("{\"kind\":\"%s\",\"expression\":\"%s\"}", kind, expression));
        return saved;
    }

    public Optional<Trigger> findById(String id) {
        return triggerRepository.findById(id);
    }

    public List<Trigger> findAll() {
        return triggerRepository.findAll();
    }

    public List<Trigger> findActive() {
        return triggerRepository.findByStatus("active");
    }

    public List<Trigger> findByAgent(String agentId) {
        return triggerRepository.findByAgentId(agentId);
    }

    @Transactional
    public Trigger pause(String id, String actor) {
        Trigger trigger = triggerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trigger not found: " + id));
        trigger.setStatus("paused");
        Trigger saved = triggerRepository.save(trigger);
        auditService.log(actor, "PAUSE_TRIGGER", "trigger", id, null);
        return saved;
    }

    @Transactional
    public Trigger resume(String id, String actor) {
        Trigger trigger = triggerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trigger not found: " + id));
        trigger.setStatus("active");
        Trigger saved = triggerRepository.save(trigger);
        auditService.log(actor, "RESUME_TRIGGER", "trigger", id, null);
        return saved;
    }

    @Transactional
    public Trigger archive(String id, String actor) {
        Trigger trigger = triggerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trigger not found: " + id));
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

            // Simple cron-like check: every minute for "*" or "*/1 * * * *" expressions
            String expr = trigger.getExpression();
            boolean fireEveryMinute = "*".equals(expr) || "*/1 * * * *".equals(expr);
            if (fireEveryMinute) {
                Instant lastFire = lastFireTimes.getOrDefault(trigger.getId(), Instant.MIN);
                if (ChronoUnit.MINUTES.between(lastFire, now) >= 1) {
                    // SUB-052: Idempotency key = trigger_id + scheduled_time
                    String idempotencyKey = trigger.getId() + ":" + now.truncatedTo(ChronoUnit.MINUTES);
                    Optional<TriggerFiring> existing = firingRepository
                            .findByTriggerIdAndIdempotencyKey(trigger.getId(), idempotencyKey);
                    if (existing.isPresent()) {
                        // Already fired — return original run (SUB-052 replay)
                        auditService.logSystem("REPLAY_FIRING", "trigger_firing", existing.get().getId(), null);
                        continue;
                    }

                    // Record firing
                    TriggerFiring firing = new TriggerFiring();
                    firing.setId(UUID.randomUUID().toString());
                    firing.setTriggerId(trigger.getId());
                    firing.setIdempotencyKey(idempotencyKey);
                    firing.setFiredAt(now);
                    firingRepository.save(firing);

                    lastFireTimes.put(trigger.getId(), now);
                    trigger.setLastFiredAt(now);
                    triggerRepository.save(trigger);
                    auditService.logSystem("FIRE_TRIGGER", "trigger", trigger.getId(), null);
                }
            }
        }
    }

    public Map<String, Object> getStats() {
        long active = triggerRepository.findByStatus("active").size();
        long paused = triggerRepository.findByStatus("paused").size();
        long archived = triggerRepository.findByStatus("archived").size();
        return Map.of("active", active, "paused", paused, "archived", archived, "total", active + paused + archived);
    }
}
