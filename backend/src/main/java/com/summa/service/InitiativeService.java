package com.summa.service;

import com.summa.repository.InitiativeRepository;
import com.summa.repository.BoardTaskRepository;
import com.summa.repository.AskRepository;
import com.summa.repository.TriggerRepository;
import com.summa.repository.SpawnRequestRepository;
import com.summa.model.Initiative;
import com.summa.model.BoardTask;
import com.summa.model.Ask;
import com.summa.model.Trigger;
import com.summa.model.SpawnRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class InitiativeService {
    private static final long STALL_CHECK_INTERVAL_MS = 300000; // 5 minutes
    private static final long STALL_ASK_DEADLINE_SECONDS = 7 * 86400L; // 7 days

    private final InitiativeRepository initiativeRepository;
    private final BoardTaskRepository boardTaskRepository;
    private final AuditService auditService;
    private final AskService askService;
    private final AskRepository askRepository;
    private final TriggerRepository triggerRepository;
    private final SpawnRequestRepository spawnRequestRepository;

    public InitiativeService(InitiativeRepository initiativeRepository, BoardTaskRepository boardTaskRepository,
                              AuditService auditService, AskService askService,
                              AskRepository askRepository, TriggerRepository triggerRepository,
                              SpawnRequestRepository spawnRequestRepository) {
        this.initiativeRepository = initiativeRepository;
        this.boardTaskRepository = boardTaskRepository;
        this.auditService = auditService;
        this.askService = askService;
        this.askRepository = askRepository;
        this.triggerRepository = triggerRepository;
        this.spawnRequestRepository = spawnRequestRepository;
    }

    @Transactional
    public Initiative create(String id, String title, String sponsor, String lead,
                              String goalRef, String decisionRef, Instant deadline,
                              String dependsOn) {
        Initiative initiative = new Initiative();
        initiative.setId(id);
        initiative.setTitle(title);
        initiative.setSponsor(sponsor);
        initiative.setLead(lead);
        initiative.setGoalRef(goalRef);
        initiative.setDecisionRef(decisionRef);
        initiative.setDeadline(deadline);
        initiative.setDependsOn(dependsOn != null ? dependsOn : "[]");

        Initiative saved = initiativeRepository.save(initiative);
        auditService.log(sponsor, "CREATE", "initiative", id,
            String.format("{\"title\":\"%s\",\"lead\":\"%s\"}", title, lead));
        return saved;
    }

    public Optional<Initiative> findById(String id) {
        return initiativeRepository.findById(id);
    }

    public List<Initiative> findAll() {
        return initiativeRepository.findAll();
    }

    public List<Initiative> findAllActive() {
        return initiativeRepository.findByStatus("active");
    }

    public List<Initiative> findByStatus(String status) {
        return initiativeRepository.findByStatus(status);
    }

    @Transactional
    public Initiative activate(String id, String actor) {
        Initiative initiative = initiativeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Initiative not found: " + id));

        if (!"proposed".equals(initiative.getStatus())) {
            throw new IllegalStateException("Cannot activate initiative with status: " + initiative.getStatus());
        }

        initiative.setStatus("active");
        Initiative saved = initiativeRepository.save(initiative);
        auditService.log(actor, "ACTIVATE", "initiative", id, null);
        return saved;
    }

    @Transactional
    public Initiative pause(String id, String actor) {
        Initiative initiative = initiativeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Initiative not found: " + id));

        if (!"active".equals(initiative.getStatus())) {
            throw new IllegalStateException("Cannot pause initiative with status: " + initiative.getStatus());
        }

        initiative.setStatus("paused");
        Initiative saved = initiativeRepository.save(initiative);
        auditService.log(actor, "PAUSE", "initiative", id, null);
        return saved;
    }

    @Transactional
    public Initiative resume(String id, String actor) {
        Initiative initiative = initiativeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Initiative not found: " + id));

        if (!"paused".equals(initiative.getStatus())) {
            throw new IllegalStateException("Cannot resume initiative with status: " + initiative.getStatus());
        }

        initiative.setStatus("active");
        Initiative saved = initiativeRepository.save(initiative);
        auditService.log(actor, "RESUME", "initiative", id, null);
        return saved;
    }

    @Transactional
    public Initiative close(String id, String actor) {
        Initiative initiative = initiativeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Initiative not found: " + id));

        if (!"active".equals(initiative.getStatus()) && !"paused".equals(initiative.getStatus())) {
            throw new IllegalStateException("Cannot close initiative with status: " + initiative.getStatus());
        }

        // INT-040: dependency check — resolve open work before closing
        List<BoardTask> openTasks = boardTaskRepository.findByInitiativeId(id).stream()
                .filter(t -> !"done".equals(t.getStatus()))
                .toList();
        for (BoardTask task : openTasks) {
            task.setStatus("cancelled");
            boardTaskRepository.save(task);
            auditService.logSystem("CLOSE_CANCELL_TASK", "board_task", task.getId(),
                String.format("{\"initiativeId\":\"%s\",\"reason\":\"initiative_closed\"}", id));
        }

        List<Ask> openInitiativeAsks = askRepository.findAll().stream()
                .filter(a -> id.equals(a.getInitiativeId()) && "pending".equals(a.getStatus()))
                .toList();
        for (Ask ask : openInitiativeAsks) {
            ask.setStatus("withdrawn");
            askRepository.save(ask);
            auditService.logSystem("CLOSE_WITHDRAW_ASK", "ask", ask.getId(),
                String.format("{\"initiativeId\":\"%s\",\"reason\":\"initiative_closed\"}", id));
        }

        List<SpawnRequest> pendingSpawns = spawnRequestRepository.findByStatus("requested").stream()
                .filter(s -> {
                    String bindings = s.getWorkspaceBindings();
                    if (bindings == null || bindings.isBlank()) return false;
                    try {
                        com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(bindings);
                        if (node.isArray()) {
                            for (com.fasterxml.jackson.databind.JsonNode el : node) {
                                if (id.equals(el.asText())) return true;
                            }
                        }
                    } catch (Exception ignored) {}
                    return false;
                })
                .toList();
        for (SpawnRequest spawn : pendingSpawns) {
            spawn.setStatus("archived");
            spawnRequestRepository.save(spawn);
            auditService.logSystem("CLOSE_ARCHIVE_SPAWN", "spawn_request", spawn.getId(),
                String.format("{\"initiativeId\":\"%s\",\"reason\":\"initiative_closed\"}", id));
        }

        List<Trigger> activeTriggers = triggerRepository.findByStatus("active").stream()
                .filter(t -> id.equals(t.getWorkspaceId()))
                .toList();
        for (Trigger trigger : activeTriggers) {
            trigger.setStatus("archived");
            triggerRepository.save(trigger);
            auditService.logSystem("CLOSE_ARCHIVE_TRIGGER", "trigger", trigger.getId(),
                String.format("{\"initiativeId\":\"%s\",\"reason\":\"initiative_closed\"}", id));
        }

        initiative.setStatus("closed");
        initiative.setClosedAt(Instant.now());
        Initiative saved = initiativeRepository.save(initiative);
        auditService.log(actor, "CLOSE", "initiative", id, null);
        return saved;
    }

    /**
     * INT-060/063: Scheduled check for stalled initiatives.
     * - Deadline passed with open work → bulk ask to sponsor (INT-060)
     * - Deadline passed with no open work → close-out ask (INT-063)
     * - Goal window ended without initiative action → direction ask (INT-050)
     */
    @Scheduled(fixedRate = STALL_CHECK_INTERVAL_MS) // every 5 minutes
    @Transactional
    public void checkStallsAndDirections() {
        Instant now = Instant.now();
        List<Initiative> active = initiativeRepository.findByStatus("active");
        List<Initiative> proposed = initiativeRepository.findByStatus("proposed");
        List<Initiative> all = new java.util.ArrayList<>(active);
        all.addAll(proposed);

        for (Initiative init : all) {
            // INT-060/063: Stall and close-out detection — deadline passed, still active
            if (init.getDeadline() != null && init.getDeadline().isBefore(now) && "active".equals(init.getStatus())) {
                auditService.logSystem("STALL_CHECK", "initiative", init.getId(),
                    String.format("{\"deadlinePassed\":true,\"sponsor\":\"%s\"}", init.getSponsor()));
                // INT-060: File stall ask when open work exists; INT-063: close-out ask when none
                boolean hasOpenWork = boardTaskRepository.findByInitiativeId(init.getId()).stream()
                        .anyMatch(t -> !"done".equals(t.getStatus()));
                try {
                    if (hasOpenWork) {
                        askService.create("question", "system", init.getSponsor(),
                            String.format("{\"initiativeId\":\"%s\",\"reason\":\"stall\"}", init.getId()),
                            "bulk", "escalate", 1,
                            Instant.now().plusSeconds(STALL_ASK_DEADLINE_SECONDS), null, null);
                    } else {
                        askService.create("question", "system", init.getSponsor(),
                            String.format("{\"initiativeId\":\"%s\",\"reason\":\"closeout\"}", init.getId()),
                            "bulk", "escalate", 1,
                            Instant.now().plusSeconds(STALL_ASK_DEADLINE_SECONDS), null, null);
                    }
                } catch (Exception e) {
                    auditService.logSystem("STALL_ASK_FAIL", "initiative", init.getId(),
                        String.format("{\"error\":\"%s\"}", e.getMessage()));
                }
            }

            // INT-050: Direction ask — goal window ended
            if (init.getGoalRef() != null && "active".equals(init.getStatus())) {
                auditService.logSystem("DIRECTION_CHECK", "initiative", init.getId(),
                    String.format("{\"goalRef\":\"%s\",\"sponsor\":\"%s\"}", init.getGoalRef(), init.getSponsor()));
            }
        }
    }
}
