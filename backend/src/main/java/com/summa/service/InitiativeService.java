package com.summa.service;

import com.summa.repository.InitiativeRepository;
import com.summa.repository.BoardTaskRepository;
import com.summa.repository.AskRepository;
import com.summa.repository.TriggerRepository;
import com.summa.repository.SpawnRequestRepository;
import com.summa.repository.DnaGoalRepository;
import com.summa.repository.DnaDecisionRepository;
import com.summa.model.Initiative;
import com.summa.model.BoardTask;
import com.summa.model.Ask;
import com.summa.model.Trigger;
import com.summa.model.SpawnRequest;
import com.summa.model.Human;
import com.summa.model.Agent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class InitiativeService {
    private static final long STALL_CHECK_INTERVAL_MS = 300000; // 5 minutes
    private static final long STALL_ASK_DEADLINE_SECONDS = 7 * 86400L; // 7 days
    private static final long STALL_ASK_DEDUP_WINDOW_SECONDS = 3600L; // 1 hour
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern KEYED_UNION_PATTERN = Pattern.compile("^[ha]?:.+$|^[a-zA-Z0-9_-]+$");

    private final InitiativeRepository initiativeRepository;
    private final BoardTaskRepository boardTaskRepository;
    private final AuditService auditService;
    private final AskService askService;
    private final AskRepository askRepository;
    private final TriggerRepository triggerRepository;
    private final SpawnRequestRepository spawnRequestRepository;
    private final DnaGoalRepository dnaGoalRepository;
    private final DnaDecisionRepository dnaDecisionRepository;
    private final DnaGoalService dnaGoalService;
    private final MemberService memberService;

    public InitiativeService(InitiativeRepository initiativeRepository, BoardTaskRepository boardTaskRepository,
                              AuditService auditService, AskService askService,
                              AskRepository askRepository, TriggerRepository triggerRepository,
                              SpawnRequestRepository spawnRequestRepository,
                              DnaGoalRepository dnaGoalRepository, DnaDecisionRepository dnaDecisionRepository,
                              DnaGoalService dnaGoalService, MemberService memberService) {
        this.initiativeRepository = initiativeRepository;
        this.boardTaskRepository = boardTaskRepository;
        this.auditService = auditService;
        this.askService = askService;
        this.askRepository = askRepository;
        this.triggerRepository = triggerRepository;
        this.spawnRequestRepository = spawnRequestRepository;
        this.dnaGoalRepository = dnaGoalRepository;
        this.dnaDecisionRepository = dnaDecisionRepository;
        this.dnaGoalService = dnaGoalService;
        this.memberService = memberService;
    }

    @Transactional
    public Initiative create(String id, String title, String sponsor, String lead,
                                String goalRef, String decisionRef, Instant deadline,
                                String dependsOn) {
        // Validate referenced entities exist
        if (goalRef != null && !goalRef.isBlank()) {
            dnaGoalRepository.findById(goalRef).orElseThrow(
                () -> new IllegalArgumentException("Goal not found: " + goalRef));
        }
        if (decisionRef != null && !decisionRef.isBlank()) {
            dnaDecisionRepository.findById(decisionRef).orElseThrow(
                () -> new IllegalArgumentException("Decision not found: " + decisionRef));
        }
        validateKeyedUnion(sponsor, "sponsor");
        validateKeyedUnion(lead, "lead");

        // INT-070: Cycle detection in depends_on — edges name non-closed rows only
        if (dependsOn != null && !dependsOn.isBlank() && !dependsOn.equals("[]")) {
            try {
                List<String> depIds = OBJECT_MAPPER.readValue(dependsOn,
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                for (String depId : depIds) {
                    findById(depId).orElseThrow(
                        () -> new IllegalArgumentException("Dependency initiative not found: " + depId));
                    Initiative dep = findById(depId).orElseThrow();
                    if ("closed".equals(dep.getStatus())) {
                        throw new IllegalArgumentException(
                            "Cannot depend on closed initiative: " + depId);
                    }
                }
                // Check for cycles using DFS
                if (wouldCreateCycle(id, depIds)) {
                    throw new IllegalArgumentException(
                        "Adding these dependencies would create a cycle: " + depIds);
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid dependsOn format: " + e.getMessage());
            }
        }

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

    /**
     * INT-070: Detect if adding deps to the new initiative (id) would create a cycle.
     * Walks the dependency graph from each dep; if any path reaches 'id', there's a cycle.
     */
    private boolean wouldCreateCycle(String newId, List<String> deps) {
        java.util.Set<String> visited = new java.util.HashSet<>();
        return hasPathTo(newId, deps, visited);
    }

    private boolean hasPathTo(String target, List<String> currentDeps, java.util.Set<String> visited) {
        for (String depId : currentDeps) {
            if (depId.equals(target)) return true;
            if (visited.contains(depId)) continue;
            visited.add(depId);
            Optional<Initiative> depOpt = initiativeRepository.findById(depId);
            if (depOpt.isPresent() && depOpt.get().getDependsOn() != null) {
                try {
                    List<String> grandchildDeps = OBJECT_MAPPER.readValue(
                        depOpt.get().getDependsOn(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                    if (hasPathTo(target, grandchildDeps, visited)) return true;
                } catch (Exception ignored) {}
            }
        }
        return false;
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

        // INT-020: If actor is not the sponsor, route an activation ask to the sponsor
        // with expiry=deny. The sponsor's own opens go active outright.
        if (!initiative.getSponsor().equals(actor)) {
            // INT-021: Re-validate goal liveness at respond time
            if (initiative.getGoalRef() != null && !initiative.getGoalRef().isBlank()) {
                Optional<com.summa.model.DnaGoal> goalOpt = dnaGoalRepository.findById(initiative.getGoalRef());
                if (goalOpt.isEmpty() || !"active".equals(goalOpt.get().getStatus())) {
                    // Goal died mid-wait: audit-only activation, file successor ask
                    auditService.logSystem("ACTIVATE_GOAL_DIED", "initiative", id,
                        String.format("{\"actor\":\"%s\",\"goalRef\":\"%s\"}", actor, initiative.getGoalRef()));
                    String payload = String.format(
                        "{\"initiativeId\":\"%s\",\"reason\":\"goal_died_during_activation\",\"goalRef\":\"%s\"}",
                        id, initiative.getGoalRef());
                    askService.create("question", "system", initiative.getSponsor(),
                        payload, "bulk", "escalate", 1,
                        Instant.now().plusSeconds(STALL_ASK_DEADLINE_SECONDS), null, null);
                    return initiative;
                }
            }
            // Route activation ask to sponsor
            String payload = String.format(
                "{\"initiativeId\":\"%s\",\"title\":\"%s\",\"createdBy\":\"%s\"}",
                id, initiative.getTitle(), actor);
            askService.create("approval", "system", initiative.getSponsor(),
                payload, "standard", "deny", 1,
                Instant.now().plusSeconds(STALL_ASK_DEADLINE_SECONDS), null, null);
            auditService.logSystem("ACTIVATE_REQUESTED", "initiative", id,
                String.format("{\"actor\":\"%s\",\"sponsor\":\"%s\"}", actor, initiative.getSponsor()));
            return initiative;
        }

        // Sponsor opening their own initiative: activate directly
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
            auditService.logSystem("CLOSE_CANCEL_TASK", "board_task", task.getId(),
                String.format("{\"initiativeId\":\"%s\",\"reason\":\"initiative_closed\"}", id));
        }

        List<Ask> openInitiativeAsks = askRepository.findByInitiativeIdAndStatusPending(id);
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
                        JsonNode node = OBJECT_MAPPER.readTree(bindings);
                        if (node.isArray()) {
                            for (JsonNode el : node) {
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

        // INT-042: File a retrospective ask (kind question, tier bulk, expiry escalate)
        // to the lead — the sponsor when the lead is non-active.
        String retrospectiveLead = isMemberActive(initiative.getLead())
            ? initiative.getLead() : initiative.getSponsor();
        try {
            String retroPayload = String.format(
                "{\"initiativeId\":\"%s\",\"title\":\"%s\",\"outcome\":\"closed\"}",
                id, initiative.getTitle());
            askService.create("question", "system", retrospectiveLead,
                retroPayload, "bulk", "escalate", 1,
                Instant.now().plusSeconds(STALL_ASK_DEADLINE_SECONDS), null, null);
        } catch (Exception e) {
            auditService.logSystem("CLOSE_RETRO_ASK_FAIL", "initiative", id,
                String.format("{\"error\":\"%s\"}", e.getMessage()));
        }

        // INT-071: Closing an upstream initiative with active dependents raises asks to each
        // dependent's sponsor — proceed, re-base, or pause.
        try {
            raiseDependentCloseAsks(id, actor);
        } catch (Exception e) {
            auditService.logSystem("CLOSE_DEPENDENT_ASKS_FAIL", "initiative", id,
                String.format("{\"error\":\"%s\"}", e.getMessage()));
        }

        initiative.setStatus("closed");
        initiative.setClosedAt(Instant.now());
        Initiative saved = initiativeRepository.save(initiative);
        auditService.log(actor, "CLOSE", "initiative", id, null);
        return saved;
    }

    /**
     * Check whether a member ID refers to an active member (non-deactivated human or active agent).
     */
    private boolean isMemberActive(String memberId) {
        if (memberId == null || memberId.isBlank()) return false;
        // Check if it's a human
        Optional<Human> humanOpt = memberService.findHuman(memberId);
        if (humanOpt.isPresent()) {
            return humanOpt.get().isActive();
        }
        // Check if it's an agent
        Optional<Agent> agentOpt = memberService.findAgent(memberId);
        if (agentOpt.isPresent()) {
            return agentOpt.get().isActive();
        }
        return false;
    }

    /**
     * INT-060/063: Scheduled check for stalled initiatives.
     * - Deadline passed with open work → bulk ask to sponsor (INT-060)
     * - Deadline passed with no open work → close-out ask (INT-063)
     * - Goal window ended without initiative action → direction ask (INT-050)
     * Deduplicates by refusing to file a new ask if one was created within
     * STALL_ASK_DEDUP_WINDOW_SECONDS for the same initiative and reason.
     */
    @Scheduled(fixedRate = STALL_CHECK_INTERVAL_MS) // every 5 minutes
    @Transactional
    public void checkStallsAndDirections() {
        Instant now = Instant.now();
        Instant dedupCutoff = now.minusSeconds(STALL_ASK_DEDUP_WINDOW_SECONDS);
        // INT-062: Clock runs while active AND proposed; paused suspends it; closed stops it
        List<Initiative> active = initiativeRepository.findByStatus("active");
        List<Initiative> proposed = initiativeRepository.findByStatus("proposed");
        List<Initiative> all = new java.util.ArrayList<>(active);
        all.addAll(proposed);

        for (Initiative init : all) {
            // INT-060/063: Stall and close-out detection — deadline passed
            // INT-062: Runs for both active and proposed states
            if (init.getDeadline() != null && init.getDeadline().isBefore(now)) {
                boolean isActive = "active".equals(init.getStatus());
                boolean isProposed = "proposed".equals(init.getStatus());
                if (isActive || isProposed) {
                    auditService.logSystem("STALL_CHECK", "initiative", init.getId(),
                        String.format("{\"deadlinePassed\":true,\"sponsor\":\"%s\",\"status\":\"%s\"}", init.getStatus(), init.getSponsor()));
                    // INT-060: File stall ask when open work exists; INT-063: close-out ask when none
                    boolean hasOpenWork = boardTaskRepository.findByInitiativeId(init.getId()).stream()
                            .anyMatch(t -> !"done".equals(t.getStatus()));
                    String stallReason = hasOpenWork ? "stall" : "closeout";
                    // Dedup: skip if a stall/close-out ask was filed recently for this initiative
                    if (!hasRecentStallAsk(init.getId(), stallReason, dedupCutoff)) {
                        try {
                            askService.create("question", "system", init.getSponsor(),
                                String.format("{\"initiativeId\":\"%s\",\"reason\":\"%s\"}", init.getId(), stallReason),
                                "bulk", "escalate", 1,
                                Instant.now().plusSeconds(STALL_ASK_DEADLINE_SECONDS), null, null);
                        } catch (Exception e) {
                            auditService.logSystem("STALL_ASK_FAIL", "initiative", init.getId(),
                                String.format("{\"error\":\"%s\"}", e.getMessage()));
                        }
                    }
                }
            }

            // INT-050: Direction ask — goal window ended
            if (init.getGoalRef() != null && !init.getGoalRef().isBlank()) {
                try {
                    Optional<com.summa.model.DnaGoal> goalOpt = dnaGoalService.findById(init.getGoalRef());
                    boolean windowEnded = goalOpt.filter(g ->
                            g.getEffectiveTo() != null && g.getEffectiveTo().isBefore(now))
                            .isPresent();
                    boolean goalTerminal = goalOpt.filter(g ->
                            !"active".equals(g.getStatus())).isPresent();
                    if (windowEnded || goalTerminal) {
                        String reason = windowEnded ? "window_ended" : "goal_terminal";
                        // Dedup: skip if a direction ask was filed recently for this initiative
                        if (!hasRecentStallAsk(init.getId(), "direction_" + reason, dedupCutoff)) {
                            String goalStatus = goalOpt.map(com.summa.model.DnaGoal::getStatus).orElse("unknown");
                            askService.create("question", "system", init.getSponsor(),
                                String.format("{\"initiativeId\":\"%s\",\"goalRef\":\"%s\",\"reason\":\"%s\",\"goalStatus\":\"%s\"}",
                                    init.getId(), init.getGoalRef(), reason, goalStatus),
                                "bulk", "escalate", 1,
                                Instant.now().plusSeconds(STALL_ASK_DEADLINE_SECONDS), null, null);
                            auditService.logSystem("DIRECTION_ask_CREATED", "initiative", init.getId(),
                                String.format("{\"goalRef\":\"%s\",\"sponsor\":\"%s\",\"reason\":\"%s\"}",
                                    init.getGoalRef(), init.getSponsor(), reason));
                        }
                    }
                } catch (Exception e) {
                    auditService.logSystem("DIRECTION_ask_FAIL", "initiative", init.getId(),
                        String.format("{\"error\":\"%s\"}", e.getMessage()));
                }
            }
        }
    }

    /**
     * Check whether a stall/direction ask for the given initiative and reason
     * was filed within the dedup window. Uses initiativeId in the ask payload match.
     */
    private boolean hasRecentStallAsk(String initiativeId, String reason, Instant cutoff) {
        try {
            List<Ask> recent = askRepository.findByInitiativeIdAndStatusPending(initiativeId);
            for (Ask ask : recent) {
                if (ask.getCreatedAt() == null || ask.getCreatedAt().isBefore(cutoff)) continue;
                try {
                    com.fasterxml.jackson.databind.JsonNode node = OBJECT_MAPPER.readTree(ask.getPayload());
                    if (node.has("reason") && node.get("reason").asText().equals(reason)) {
                        return true;
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void validateKeyedUnion(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (!KEYED_UNION_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a valid keyed union (h:<human-id> or a:<agent-id>)");
        }
    }

    /**
     * INT-071: When an initiative closes, file bulk-tier escalation asks to each dependent's
     * sponsor: proceed, re-base (dependency edge re-pointed), or pause.
     */
    private void raiseDependentCloseAsks(String closedId, String actor) {
        List<Initiative> allInitiatives = findAll();
        for (Initiative dep : allInitiatives) {
            if ("closed".equals(dep.getStatus())) continue;
            if (dep.getId().equals(closedId)) continue;
            if (dep.getDependsOn() == null || dep.getDependsOn().isBlank()) continue;
            try {
                List<String> deps = OBJECT_MAPPER.readValue(dep.getDependsOn(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                if (deps.contains(closedId)) {
                    // File coordination ask to the dependent's sponsor
                    String askTo = isMemberActive(dep.getSponsor())
                        ? dep.getSponsor() : OffboardingWalkService.ADMIN_BROADCAST;
                    String payload = String.format(
                        "{\"initiativeId\":\"%s\",\"upstreamClosed\":\"%s\",\"reason\":\"upstream_closed\"}",
                        dep.getId(), closedId);
                    askService.create("question", "system", askTo,
                        payload, "bulk", "escalate", 1,
                        Instant.now().plusSeconds(STALL_ASK_DEADLINE_SECONDS), dep.getId(), null);
                    auditService.logSystem("DEPENDENT_CLOSE_ASK", "initiative", dep.getId(),
                        String.format("{\"upstreamClosed\":\"%s\",\"sponsor\":\"%s\"}", closedId, askTo));
                }
            } catch (Exception ignored) {}
        }
    }
}
