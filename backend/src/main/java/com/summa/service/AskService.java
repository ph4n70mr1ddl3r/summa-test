package com.summa.service;

import com.summa.enums.RbacRole;
import com.summa.constants.Defaults;
import com.summa.repository.AskRepository;
import com.summa.model.Ask;
import com.summa.model.ExpiryBehavior;
import com.summa.model.Human;
import com.summa.repository.InitiativeRepository;
import com.summa.repository.WorkspaceRepository;
import com.summa.repository.DnaDomainRepository;
import com.summa.model.DnaDomain;
import com.summa.model.Workspace;
import com.summa.model.Initiative;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;
import com.summa.enums.AskKind;
import com.summa.enums.AskStatus;
import com.summa.enums.AskTier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.summa.exception.EntityNotFoundException;
import com.summa.util.JsonHelpers;

/**
 * Bounded map entry with expiry support for storm collapse tracking.
 */
class ExpiringEntry<V> {
    final V value;
    final long expiryEpochSeconds;
    ExpiringEntry(V value, long expiryEpochSeconds) {
        this.value = value;
        this.expiryEpochSeconds = expiryEpochSeconds;
    }
}

@Service
public class AskService {
    private final AskRepository askRepository;
    private final AuditService auditService;
    private final MemberService memberService;
    private final GovernanceService governanceService;
    private final InitiativeRepository initiativeRepository;
    private final WorkspaceRepository workspaceRepository;
    private final DnaDomainRepository domainRepository;
    private final InitiativeService initiativeService;
    private final long stormCollapseWindowSeconds;

    // ASK-100: Storm collapse window — tracks recent ask creation by (kind, to, payloadHash)
    // Uses bounded maps with TTL-based eviction to prevent unbounded memory growth.
    // NOTE: These maps are process-local. In a multi-instance deployment, each instance
    // maintains its own collapse window, which may lead to duplicate asks across instances.
    // A distributed cache (e.g., Redis) is required for correct storm collapse in multi-instance mode.
    private final ConcurrentHashMap<String, ExpiringEntry<Instant>> collapseWindowTimestamps = new ConcurrentHashMap<>();
    // ASK-057: Successor depth tracking uses a much longer TTL than the storm collapse window
    // so chain depth is preserved across the full lifetime of an escalation chain.
    private static final long SUCCESSOR_DEPTH_TTL_SECONDS = Defaults.SECONDS_PER_YEAR;
    private final ConcurrentHashMap<String, ExpiringEntry<Integer>> successorDepth = new ConcurrentHashMap<>();
    // Per-key locks to prevent concurrent collapse race conditions
    private final ConcurrentHashMap<String, ReentrantLock> collapseLocks = new ConcurrentHashMap<>();
    // Guards the depth check-read-bump per chain root so concurrent expiries (the
    // scheduled sweep and the manual expireNow door) cannot each read the same
    // depth and blow past MAX_EXPIRE_SUCCESSOR_DEPTH (ASK-057 chain exhaustion).
    private final ConcurrentHashMap<String, ReentrantLock> depthLocks = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public AskService(AskRepository askRepository, AuditService auditService, MemberService memberService,
                      GovernanceService governanceService, InitiativeRepository initiativeRepository,
                      WorkspaceRepository workspaceRepository, DnaDomainRepository domainRepository,
                      @Lazy InitiativeService initiativeService,
                      @Value("${summa.asks.storm-collapse-window-hours:1}") long stormCollapseWindowHours,
                      ObjectMapper objectMapper) {
        this.askRepository = askRepository;
        this.auditService = auditService;
        this.memberService = memberService;
        this.governanceService = governanceService;
        this.initiativeRepository = initiativeRepository;
        this.workspaceRepository = workspaceRepository;
        this.domainRepository = domainRepository;
        this.initiativeService = initiativeService;
        this.stormCollapseWindowSeconds = stormCollapseWindowHours * 3600L;
        this.objectMapper = objectMapper;
    }

    /**
     * API-061: validation before constraint — the ask's optional references are
     * foreign-key-pinned, so an unknown one aborts the insert as an integrity
     * violation (a 409) rather than the validation refusal the API owes.
     */
    @Transactional(readOnly = true)
    public void requireLiveReferences(String initiativeId, String workspaceId) {
        if (initiativeId != null && !initiativeId.isBlank()) {
            if (!initiativeRepository.existsById(initiativeId)) {
                throw new IllegalArgumentException(
                        "initiativeId does not reference an existing initiative: " + initiativeId);
            }
        }
        if (workspaceId != null && !workspaceId.isBlank()) {
            if (!workspaceRepository.existsById(workspaceId)) {
                throw new IllegalArgumentException(
                        "workspaceId does not reference an existing workspace: " + workspaceId);
            }
        }
    }

    /**
     * Purge expired entries from collapse window, successor depth, and lock maps.
     * Called periodically to prevent unbounded memory growth.
     */
    @Scheduled(fixedRate = 3600000)
    public void purgeExpiredEntries() {
        long nowSeconds = Instant.now().getEpochSecond();
        collapseWindowTimestamps.entrySet().removeIf(e -> e.getValue().expiryEpochSeconds <= nowSeconds);
        successorDepth.entrySet().removeIf(e -> e.getValue().expiryEpochSeconds <= nowSeconds);
        // Purge locks whose associated window has expired, doing so atomically
        // to prevent a TOCTOU race where a new entry is added between the check
        // and the removal.
        List<String> staleKeys = new ArrayList<>();
        for (String key : collapseLocks.keySet()) {
            ExpiringEntry<Instant> entry = collapseWindowTimestamps.get(key);
            if (entry == null || entry.expiryEpochSeconds <= nowSeconds) {
                staleKeys.add(key);
            }
        }
        for (String key : staleKeys) {
            collapseLocks.remove(key);
        }
    }

    @Transactional
    public Ask create(String kind, String from, String to, String payload, String slaTier,
                      String expiryBehavior, Integer quorumRequired, Instant deadline,
                      String initiativeId, String workspaceId) {
        return create(kind, from, to, payload, slaTier, expiryBehavior, quorumRequired,
                deadline, initiativeId, workspaceId, null);
    }

    @Transactional
    public Ask create(String kind, String from, String to, String payload, String slaTier,
                      String expiryBehavior, Integer quorumRequired, Instant deadline,
                      String initiativeId, String workspaceId, String escalation) {
        return createInternal(kind, from, to, payload, slaTier, expiryBehavior,
                quorumRequired, deadline, initiativeId, workspaceId, escalation, null).ask();
    }

    /**
     * A create's outcome: the canonical ask plus whether the storm-collapse path
     * attached the caller to an existing pending ask instead of inserting a row.
     * The expiry successor door needs the distinction so a collapsed successor
     * does not consume the chain's depth budget.
     */
    record CreatedAsk(Ask ask, boolean collapsed) {}

    private CreatedAsk createInternal(String kind, String from, String to, String payload, String slaTier,
                      String expiryBehavior, Integer quorumRequired, Instant deadline,
                      String initiativeId, String workspaceId, String escalation,
                      String excludeAskId) {
        // ASK-012: explicit deadline earlier than creation is refused
        if (deadline == null) {
            throw new IllegalArgumentException("Deadline is required");
        }
        Instant currentInstant = Instant.now();
        if (deadline.isBefore(currentInstant)) {
            throw new IllegalArgumentException("Deadline must be in the future");
        }

        // Service-level validation for defense-in-depth
        if (AskKind.fromValue(kind) == null) {
            throw new IllegalArgumentException("Invalid ask kind: " + kind);
        }
        // The tier is optional at the API door (the controller validates it only
        // when supplied) and the row defaults to 'standard' — so an absent tier
        // must resolve to the default, not be refused as an unknown value.
        String effectiveTier = (slaTier == null || slaTier.isBlank())
                ? AskTier.STANDARD.getValue() : slaTier;
        if (AskTier.fromValue(effectiveTier) == null) {
            throw new IllegalArgumentException("Invalid SLA tier: " + slaTier);
        }
        // Cap deadline against org-wide maximum to prevent misconfigured tiers from
        // producing deadlines beyond MAX_DEADLINE_SECONDS.
        if (deadline.getEpochSecond() - currentInstant.getEpochSecond() > Defaults.MAX_DEADLINE_SECONDS) {
            throw new IllegalArgumentException(
                "Deadline exceeds maximum allowed span of " + Defaults.MAX_DEADLINE_SECONDS + " seconds");
        }

        Ask ask = new Ask();
        ask.setId(UUID.randomUUID().toString());
        ask.setKind(kind);
        ask.setFrom(from);
        ask.setTo(to);
        ask.setPayload(payload != null ? payload : "{}");
        ask.setSlaTier(effectiveTier);
        ask.setExpiryBehavior(expiryBehavior != null && !expiryBehavior.isBlank()
                ? ExpiryBehavior.requireFromValue(expiryBehavior) : ExpiryBehavior.DENY);
        ask.setQuorumRequired(quorumRequired != null ? quorumRequired : 1);
        ask.setDeadline(deadline);
        ask.setInitiativeId(initiativeId);
        ask.setWorkspaceId(workspaceId);
        ask.setEscalation(escalation);

        String collapseKey = buildCollapseKey(kind, to, payload);
        Instant now = Instant.now();
        long nowSeconds = now.getEpochSecond();
        // Use computeIfAbsent to atomically establish the window.
        // The expiry check and DB query both run under the per-key lock to prevent
        // concurrent requests from both passing the expiry check and creating duplicates.
        ReentrantLock lock = collapseLocks.computeIfAbsent(collapseKey, k -> new ReentrantLock());
        lock.lock();
        try {
            ExpiringEntry<Instant> slotValue = collapseWindowTimestamps.computeIfAbsent(collapseKey, k -> new ExpiringEntry<>(now, nowSeconds + stormCollapseWindowSeconds));
            if (nowSeconds < slotValue.expiryEpochSeconds) {
                // Collapse: increment collapsed_count on nearest pending canonical.
                // Filter by kind, to, AND payload hash to avoid collapsing unrelated asks
                // to the same recipient within the storm window.
                List<Ask> candidates = askRepository.findByToAndStatusPending(to);
                String targetHash = buildCollapseKey(kind, to, payload);
                candidates = candidates.stream()
                    .filter(a -> excludeAskId == null || !excludeAskId.equals(a.getId()))
                    .filter(a -> targetHash.equals(buildCollapseKey(kind, to, a.getPayload())))
                    .toList();
                if (!candidates.isEmpty()) {
                    Ask canonical = candidates.get(0);
                    int currentCount = canonical.getCollapsedCount() != null ? canonical.getCollapsedCount() : 0;
                    canonical.setCollapsedCount(currentCount + 1);
                    Ask saved = askRepository.save(canonical);
                    auditService.log(from, "COLLAPSED_ASK", "ask", saved.getId(),
                        String.format("{\"collapsedCount\":%d}", saved.getCollapsedCount() != null ? saved.getCollapsedCount() : 0));
                    flagQuorumPoolShortfall(saved);
                    return new CreatedAsk(saved, true);
                }
            }

            // Save inside the lock to prevent concurrent duplicates from passing the collapse check
            Ask saved = askRepository.save(ask);
            // ASK-053: N is pool-checked at every write door — an N larger than the
            // eligible approver pool flags at write like any contradiction. The ask
            // is still filed (a live pool can grow back — ASK-044), but the
            // shortfall is named so the write is never a silent impossibility.
            flagQuorumPoolShortfall(saved);
            // Re-arm the window for this new canonical ask: computeIfAbsent above does not
            // refresh an expired entry, so without this put every ask created after the
            // first window expiry would pass the collapse check forever (until the hourly
            // purge evicts the stale slot) and storms would never collapse again.
            collapseWindowTimestamps.put(collapseKey, new ExpiringEntry<>(now, nowSeconds + stormCollapseWindowSeconds));
            auditService.log(from, "CREATE", "ask", ask.getId(),
                String.format("{\"kind\":%s,\"to\":%s,\"tier\":%s}", JsonHelpers.jsonString(kind), JsonHelpers.jsonString(to), JsonHelpers.jsonString(ask.getSlaTier())));
            return new CreatedAsk(saved, false);
        } finally {
            lock.unlock();
        }
    }

    /**
     * ASK-053: "N is bounded below (`requires_approvals` ≥ 1) and pool-checked at
     * every write door: N exceeding the eligible approver pool flags at proposal
     * time like any contradiction". The flag names the required quorum, the
     * evaluated pool and the gap between them — the same shape the expiry
     * escalation uses when the pool has shrunk under N.
     *
     * <p>The ask is not refused: a live pool can grow back (ASK-044), so the
     * write stays legal and the impossibility is surfaced instead of hidden.
     */
    private void flagQuorumPoolShortfall(Ask ask) {
        int quorum = ask.getQuorumRequired() != null ? ask.getQuorumRequired() : 1;
        if (quorum <= 1) return;
        int pool = eligiblePoolSize(ask);
        if (quorum <= pool) return;
        auditService.logSystem("ASK_QUORUM_POOL_SHORTFALL", "ask", ask.getId(),
            String.format("{\"quorumRequired\":%d,\"eligiblePool\":%d,\"shortfall\":%d}",
                quorum, pool, quorum - pool));
    }

    private String buildCollapseKey(String kind, String to, String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((kind + "|" + to + "|" + (payload != null ? payload : "")).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Ask> findById(String id) {
        return askRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Ask> findByStatus(String status) {
        return askRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Ask> findByTo(String to, int limit) {
        return askRepository.findByToAndStatusPending(to, limit);
    }

    @Transactional(readOnly = true)
    public List<Ask> findByToAndStatusPending(String to) {
        return askRepository.findByToAndStatusPending(to);
    }

    @Transactional(readOnly = true)
    public List<Ask> findByFromAndStatusPending(String from) {
        return askRepository.findByFromAndStatusPending(from);
    }

    @Transactional(readOnly = true)
    public List<Ask> findByStatus(String status, int limit) {
        return askRepository.findByStatusOrdered(status, limit);
    }

    @Transactional(readOnly = true)
    public List<Ask> findByToAndStatus(String to, String status, int limit) {
        return askRepository.findByToAndStatusOrdered(to, status, limit);
    }

    @Transactional(readOnly = true)
    public List<Ask> findAllPending(int limit) {
        return askRepository.findByStatusPendingOrdered(limit);
    }

    @Transactional(readOnly = true)
    public List<Ask> findExpired() {
        return askRepository.findExpiredBefore(Instant.now());
    }

    /**
     * ASK-011: Periodically expire asks whose deadline has passed.
     * Each expired ask is processed with individual error isolation via try/catch.
     * - deny: closes as expired
     * - escalate: closes as expired, files successor to escalation target
     * - reassign: closes as expired, files successor to deputy/admin
     * ASK-057: Chain exhaustion — if no active recipient found, broadcasts org-stall alert.
     */
    @Transactional
    @Scheduled(fixedRate = 60000)
    public void processExpiredAsks() {
        List<Ask> expired = askRepository.findExpiredBefore(Instant.now());
        for (Ask ask : expired) {
            try {
                // CG1: Re-fetch to detect concurrent processing — skip if already handled.
                // Without this, two scheduled executions could both find the same ask
                // and create duplicate successor asks or double-expire.
                Optional<Ask> freshOpt = askRepository.findById(ask.getId());
                if (freshOpt.isEmpty()) continue;
                Ask current = freshOpt.get();
                if (!current.isPending()) continue;
                String behavior = current.getExpiryBehavior() != null ? current.getExpiryBehavior() : "deny";
                if ("deny".equals(behavior)) {
                    expire(current.getId());
                } else if ("escalate".equals(behavior) || "reassign".equals(behavior)) {
                    try {
                        // The shared successor door consults the chain-depth budget
                        // (ASK-057), carries the chain root on the successor so depth
                        // accumulates across both doors, and walks the deputy hop
                        // (ASK-060) — see openExpirySuccessor.
                        openExpirySuccessor(current, behavior, Defaults.SYSTEM_ACTOR);
                    } catch (Exception ex) {
                        auditService.logSystem("EXPIRE_SUCCESSOR_FAIL", "ask", current.getId(),
                            JsonHelpers.toJson(Map.of("behavior", behavior, "error", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()), objectMapper));
                    }
                    // Expire the original ask even when the successor door throws —
                    // an unexpired ask must not linger past its deadline just because
                    // the chain hop failed. Matches expireNow() which always expires.
                    expire(current.getId());
                }
            } catch (Exception e) {
                auditService.logSystem("EXPIRE_BATCH_FAIL", "ask", ask.getId(),
                    JsonHelpers.toJson(Map.of("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), objectMapper));
            }
        }
    }

    /**
     * ASK-057/058: Broadcast org-stall alert to every active human when an ask chain exhausts.
     * The alert is a critical-tier question ask rendered to all active humans including viewers.
     */
    private void broadcastOrgStall(Ask originalAsk) {
        try {
            Map<String, Object> payloadMap = new LinkedHashMap<>();
            payloadMap.put("originalAskId", originalAsk.getId());
            payloadMap.put("originalAskKind", originalAsk.getKind());
            payloadMap.put("originalAskTier", originalAsk.getSlaTier());
            payloadMap.put("reason", "chain_exhausted");
            String payload = JsonHelpers.toJson(payloadMap, objectMapper);
            create(AskKind.QUESTION.getValue(), "system", OffboardingWalkService.ADMIN_BROADCAST,
                payload, "critical", "deny", 1,
                Instant.now().plusSeconds(Defaults.DEFAULT_CRITICAL_ASK_DEADLINE_HOURS * 3600L),
                originalAsk.getInitiativeId(), originalAsk.getWorkspaceId());
            auditService.logSystem("ORG_STALL_BROADCAST", "ask", originalAsk.getId(),
                String.format("{\"reason\":\"chain_exhausted\",\"originalAskId\":\"%s\"}", originalAsk.getId()));
        } catch (Exception e) {
            auditService.logSystem("ORG_STALL_BROADCAST_FAIL", "ask", originalAsk.getId(),
                JsonHelpers.toJson(Map.of("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), objectMapper));
        }
    }

    /**
     * ASK-040/050/052: Respond to an ask with eligibility, quorum, and re-validation checks.
     * ASK-052: When N>1, only pool principals' accepts count — deputy/delegate accepts are audit-only.
     *
     * <p>Without an explicit decision the response is an accept (the historical
     * free-text response) — a caller that has no decision to express records an
     * acceptance, never a denial.
     */
    @Transactional
    public Ask respond(String id, String responder, String response) {
        return respond(id, responder, response, null);
    }

    /**
     * ASK-050: the response carries a decision ({@code accept} | {@code deny}).
     * A deny closes the ask denied immediately; only accepts count toward the quorum.
     */
    @Transactional
    public Ask respond(String id, String responder, String response, String decision) {
        Ask ask = askRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ask not found: " + id));

        String effectiveDecision = normalizeDecision(decision);

        if (!ask.isPending()) {
            // ASK-015: later responses to an already-closed ask are audit-only, not refused
            auditService.logSystem("AUDIT_ONLY_STALE_RESPONSE", "ask", id,
                String.format("{\"responder\":%s,\"currentStatus\":%s}", JsonHelpers.jsonString(responder), JsonHelpers.jsonString(ask.getStatus())));
            return ask;
        }

        // ASK-015/ASK-050: a response to an expired ask is recorded with no effect —
        // expiry denies. The deadline is a hard cut, not a sweep-granularity
        // boundary: between the deadline and the next sweep tick the ask is still
        // 'pending', and a late accept must not bind and close it 'answered'.
        if (ask.getDeadline() != null && ask.getDeadline().isBefore(Instant.now())) {
            auditService.logSystem("AUDIT_ONLY_STALE_RESPONSE", "ask", id,
                String.format("{\"responder\":%s,\"currentStatus\":%s,\"reason\":\"past_deadline\"}",
                    JsonHelpers.jsonString(responder), JsonHelpers.jsonString(ask.getStatus())));
            return ask;
        }

        // ASK-040: Check eligibility at the door — a response from outside the
        // ask's eligible set is refused with the attempt audited.
        if (!isEligibleResponder(ask, responder)) {
            auditService.logSystem("REFUSAL", "ask_respond", id,
                String.format("{\"responder\":%s,\"reason\":%s}",
                    JsonHelpers.jsonString(responder),
                    JsonHelpers.jsonString("responder_not_eligible")));
            throw new IllegalArgumentException("Responder is not eligible: " + responder);
        }

        int quorum = ask.getQuorumRequired() != null ? ask.getQuorumRequired() : 1;

        // ASK-052: When N>1, only pool principals' accepts count; deputy/delegate is audit-only
        if (quorum > 1) {
            boolean isPoolPrincipal = isPoolPrincipal(ask, responder);
            if (!isPoolPrincipal) {
                // Audit-only: record but don't count toward quorum
                auditService.logSystem("AUDIT_ONLY_QUORUM_RESPONSE", "ask", id,
                    String.format("{\"responder\":%s,\"reason\":\"not_pool_principal\",\"decision\":%s}",
                        JsonHelpers.jsonString(responder), JsonHelpers.jsonString(effectiveDecision)));
                return ask;
            }
        }

        // ASK-050: a deny closes the ask denied immediately — the terminal is not
        // "answered", and the denial never counts toward a quorum. A deny from a
        // pool principal is binding whatever else the ledger holds.
        if (AskDecision.DENY.getValue().equals(effectiveDecision)) {
            recordResponse(ask, responder, response, effectiveDecision);
            ask.setRespondedAt(Instant.now());
            ask.setStatus(AskStatus.DENIED.getValue());
            Ask saved = askRepository.save(ask);
            auditService.log(responder, "RESPOND_DENY", "ask", id,
                String.format("{\"decision\":%s}", JsonHelpers.jsonString(effectiveDecision)));
            return saved;
        }

        // ASK-015: quorum-1 closes on first response; later responses are audit-only
        if (quorum == 1) {
            recordResponse(ask, responder, response, effectiveDecision);
            ask.setRespondedAt(Instant.now());
            ask.setStatus(AskStatus.ANSWERED.getValue());
            Ask saved = askRepository.save(ask);
            auditService.log(responder, "RESPOND", "ask", id,
                String.format("{\"response\":%s}", JsonHelpers.jsonString(response != null && !response.isEmpty() ? response.substring(0, Math.min(100, response.length())) : "")));
            // INT-051: Direction ask response — move linkage atomically
            handleDirectionAskResponse(ask, response);
            return saved;
        }

        // ASK-050: N > 1 quorum — collect accepts until N distinct principals accepted
        List<String> existingResponses = parseResponseIds(ask);
        // Normalize the responder through stripIdPrefix so quorum counting compares
        // clean forms even when the caller supplied a keyed-union value.
        String responderClean = JsonHelpers.stripIdPrefix(responder);
        if (existingResponses.stream().anyMatch(r -> JsonHelpers.stripIdPrefix(r).equals(responderClean))) {
            // Already responded — audit-only duplicate
            auditService.logSystem("AUDIT_ONLY_DUPLICATE_RESPONSE", "ask", id,
                String.format("{\"responder\":%s}", JsonHelpers.jsonString(responder)));
            return ask;
        }

        existingResponses.add(responderClean);
        String newResponsesJson = toJsonResponseList(existingResponses, response, ask.getResponses(), effectiveDecision);
        ask.setResponses(newResponsesJson);
        // Count quorum from the serialized JSON, not the in-memory list, so a
        // malformed prior `responses` column (which parseResponseIds resets to
        // empty) cannot close a multi-quorum ask on a single response. Only
        // accepts count toward the quorum (ASK-050) — a denial never does.
        int storedCount = parseAcceptResponderIds(ask).size();
        boolean quorumReached = storedCount >= quorum;
        if (quorumReached) {
            ask.setStatus(AskStatus.ANSWERED.getValue());
            ask.setRespondedAt(Instant.now());
            // INT-051: Direction ask response on quorum reach
            handleDirectionAskResponse(ask, response);
        }
        Ask saved = askRepository.save(ask);

        auditService.log(responder, "RESPOND", "ask", id,
                String.format("{\"response\":%s,\"quorumProgress\":%d/%d}",
                    JsonHelpers.jsonString(response != null && !response.isEmpty() ? response.substring(0, Math.min(100, response.length())) : ""),
                    storedCount, quorum));
        return saved;
    }

    /**
     * The decision vocabulary on the respond door (ASK-050). An absent decision is
     * an acceptance — the historical free-text response carried no decision and
     * never denied.
     */
    public enum AskDecision {
        ACCEPT("accept"),
        DENY("deny");

        private final String value;

        AskDecision(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Normalize the respond door's decision. An absent or blank decision is an
     * acceptance; an unknown one is refused at the door (API-061's validation
     * family) rather than silently read as something else.
     */
    private String normalizeDecision(String decision) {
        if (decision == null || decision.isBlank()) {
            return AskDecision.ACCEPT.getValue();
        }
        String clean = decision.trim().toLowerCase(java.util.Locale.ROOT);
        if (!AskDecision.ACCEPT.getValue().equals(clean) && !AskDecision.DENY.getValue().equals(clean)) {
            throw new IllegalArgumentException(
                "Invalid decision: " + decision + ". Must be one of: accept, deny");
        }
        return clean;
    }

    /**
     * ASK-052: Determine if a responder is a pool principal (not a deputy or delegated agent).
     * Pool principals are the target human, active admins (for admin broadcast), and domain owners.
     */
    private boolean isPoolPrincipal(Ask ask, String responder) {
        String responderClean = JsonHelpers.stripIdPrefix(responder);
        String targetClean = JsonHelpers.stripIdPrefix(ask.getTo());
        // Direct target match: target stored as keyed union (h:<id>/a:<id>); compare cleaned forms
        if (targetClean != null && targetClean.equals(responderClean)
                && !OffboardingWalkService.ADMIN_BROADCAST.equals(targetClean)) {
            Optional<Human> target = memberService.findHuman(targetClean);
            if (target.isPresent()) {
                return !RbacRole.VIEWER.getValue().equals(target.get().getRbac());
            }
            // ASK-050/052/092: toward N > 1 only human members count — "human members
            // only toward N > 1", a delegated agent's accept is "audit-only, exactly
            // like a deputy's". An agent addressee is therefore not a pool
            // principal: its accept records in the ledger and never counts.
            return false;
        }
        // Admin broadcast: any active admin is a pool principal.
        // targetClean strips the h:/a: prefix from the stored 'to' value, so a
        // keyed-union stored target of "admins" (unlikely but possible) still matches.
        if (OffboardingWalkService.ADMIN_BROADCAST.equals(ask.getTo())
                || OffboardingWalkService.ADMIN_BROADCAST.equals(targetClean)) {
            return memberService.findAdmins().stream()
                    .anyMatch(h -> h.getId().equals(responderClean));
        }
        // ASK-051: the eligible pool of a quorum ask is the pool's primary recipient
        // plus every active admin, evaluated at respond time. Without this an
        // N>1 ask addressed to a single owner can never reach quorum — the owner
        // alone cannot supply N approvals and every admin was refused at the door.
        // ASK-054: the admin the SLA breach escalates to may contribute one of the
        // required approvals.
        return isActiveAdmin(responderClean);
    }

    /**
     * Is the responder an active admin? Admin authority is evaluated against live
     * state (OFB-015): a deactivated admin contributes nothing (ASK-055: a departed
     * one contributes nothing).
     */
    private boolean isActiveAdmin(String responderClean) {
        if (responderClean == null) return false;
        return memberService.findAdmins().stream()
                .anyMatch(h -> h.getId().equals(responderClean));
    }

    /**
     * INT-051: When a direction ask (kind=question, tier=bulk, expiry=escalate) tied to an
     * initiative is answered, atomically update the initiative's goal_ref per the response.
     * The response payload carries the choice: extend, re-base, re-target, or close.
     */
    @Transactional
    public void handleDirectionAskResponse(Ask ask, String response) {
        if (ask.getInitiativeId() == null || ask.getInitiativeId().isBlank()) return;
        if (AskKind.QUESTION != AskKind.fromValue(ask.getKind()) || AskTier.BULK != AskTier.fromValue(ask.getSlaTier())) return;

        try {
            JsonNode payload = objectMapper.readTree(ask.getPayload());
            String initiativeId = ask.getInitiativeId();
            String action = response != null ? response.trim() : "";

            if ("extend".equals(action)) {
                // Extend re-windows the same goal row — no goal_ref change needed
                auditService.logSystem("DIRECTION_EXTEND", "ask", ask.getId(),
                    String.format("{\"initiativeId\":%s}", JsonHelpers.jsonString(initiativeId)));
            } else if ("close".equals(action)) {
                // Close the initiative — INT-051 requires atomic close on quorum reach
                try {
                    initiativeService.close(initiativeId, "system");
                    auditService.logSystem("DIRECTION_CLOSE", "ask", ask.getId(),
                        String.format("{\"initiativeId\":%s}", JsonHelpers.jsonString(initiativeId)));
                } catch (Exception closeEx) {
                    auditService.logSystem("DIRECTION_CLOSE_FAIL", "ask", ask.getId(),
                        String.format("{\"initiativeId\":%s,\"error\":%s}", JsonHelpers.jsonString(initiativeId), JsonHelpers.jsonString(closeEx.getMessage())));
                }
            } else if ("re-base".equals(action) || "rebase".equals(action)) {
                updateGoalRef(initiativeId, payload, "DIRECTION_REBASE");
            } else if ("re-target".equals(action) || "retarget".equals(action)) {
                updateGoalRef(initiativeId, payload, "DIRECTION_RETARGET");
            }
        } catch (Exception e) {
            auditService.logSystem("DIRECTION_RESPOND_FAIL", "ask", ask.getId(),
                String.format("{\"error\":%s}", JsonHelpers.jsonString(e.getMessage() != null ? e.getMessage() : "unknown")));
        }
    }

    private void updateGoalRef(String initiativeId, JsonNode payload, String auditAction) {
        if (payload.has("newGoalRef") && !payload.get("newGoalRef").isNull()) {
            String newGoalRef = payload.get("newGoalRef").asText();
            Optional<Initiative> initOpt = initiativeRepository.findById(initiativeId);
            if (initOpt.isPresent()) {
                initOpt.get().setGoalRef(newGoalRef);
                initiativeRepository.save(initOpt.get());
                auditService.logSystem(auditAction, "ask", initiativeId,
                    String.format("{\"initiativeId\":%s,\"newGoalRef\":%s}", JsonHelpers.jsonString(initiativeId), JsonHelpers.jsonString(newGoalRef)));
            }
        }
    }

    /**
     * ASK-012/CFG-140: Derive deadline seconds from SLA tier defaults.
     */
    public long deriveDeadlineFromTier(String tier) {
        if (tier == null) tier = "standard";
        Map<String, Long> tierDefaults = Map.of(
            "critical", Defaults.DEFAULT_CRITICAL_ASK_DEADLINE_HOURS,
            "bulk", Defaults.DEFAULT_BULK_ASK_DEADLINE_HOURS,
            "standard", Defaults.DEFAULT_STANDARD_ASK_DEADLINE_HOURS
        );
        Long defaultHours = tierDefaults.getOrDefault(tier, Defaults.DEFAULT_STANDARD_ASK_DEADLINE_HOURS);
        Number val = governanceService.getSetting("asks-tier-" + tier + "-deadline-hours", Number.class);
        if (val != null) {
            long result = val.longValue() * 3600L;
            return result > 0 ? result : (defaultHours > 0 ? defaultHours * 3600L : Defaults.MAX_DEADLINE_SECONDS);
        }
        long defaultResult = defaultHours * 3600L;
        return defaultResult > 0 ? defaultResult : Defaults.MAX_DEADLINE_SECONDS;
    }

    private void recordResponse(Ask ask, String responder, String response) {
        recordResponse(ask, responder, response, AskDecision.ACCEPT.getValue());
    }

    private void recordResponse(Ask ask, String responder, String response, String decision) {
        List<String> ids = parseResponseIds(ask);
        ids.add(responder);
        ask.setResponses(toJsonResponseList(ids, response, ask.getResponses(), decision));
    }

    private List<String> parseResponseIds(Ask ask) {
        return new ArrayList<>(responseEntries(ask).stream()
                .map(entry -> entry.get("responder"))
                .filter(r -> r != null)
                .toList());
    }

    /**
     * ASK-050: the responders whose recorded decision is an acceptance. Only these
     * count toward the quorum. A ledger entry written before the decision field
     * existed carried an acceptance (the historical free-text response), so it
     * counts as one.
     */
    private List<String> parseAcceptResponderIds(Ask ask) {
        return responseEntries(ask).stream()
                .filter(entry -> {
                    String decision = entry.get("decision");
                    return decision == null || AskDecision.ACCEPT.getValue().equals(decision);
                })
                .map(entry -> entry.get("responder"))
                .filter(r -> r != null)
                .toList();
    }

    private List<Map<String, String>> responseEntries(Ask ask) {
        try {
            String resp = ask.getResponses();
            if (resp == null || resp.isBlank() || resp.equals("[]")) return new ArrayList<>();
            List<Map<String, String>> entries = objectMapper.readValue(resp, new TypeReference<List<Map<String, String>>>() {});
            List<Map<String, String>> safe = new ArrayList<>();
            for (Map<String, String> entry : entries) {
                if (entry != null) safe.add(entry);
            }
            return safe;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJsonResponseList(List<String> ids, String latestResponse, String existingJson) {
        return toJsonResponseList(ids, latestResponse, existingJson, AskDecision.ACCEPT.getValue());
    }

    private String toJsonResponseList(List<String> ids, String latestResponse, String existingJson, String decision) {
        // Preserve prior response texts so that each responder's text is retained.
        Map<String, String> prior = new HashMap<>();
        Map<String, String> priorDecision = new HashMap<>();
        if (existingJson != null && !existingJson.isBlank() && !existingJson.equals("[]")) {
            try {
                List<Map<String, String>> entries = objectMapper.readValue(existingJson,
                        new TypeReference<List<Map<String, String>>>() {});
                for (Map<String, String> entry : entries) {
                    if (entry == null) continue;
                    String r = entry.get("responder");
                    String t = entry.get("response");
                    if (r != null) prior.put(r, t != null ? t : "");
                    if (r != null) priorDecision.put(r, entry.get("decision"));
                }
            } catch (Exception ignored) {
                // On parse failure fall back to empty prior map.
            }
        }
        // Track which responder is newly added (last in list but not in prior) so only
        // that responder receives latestResponse, not every missing responder.
        String newResponder = null;
        for (String id : ids) {
            if (!prior.containsKey(id)) {
                newResponder = id;
            }
        }
        List<Map<String, String>> list = new ArrayList<>();
        for (String id : ids) {
            Map<String, String> entry = new HashMap<>();
            entry.put("responder", id);
            if (prior.containsKey(id)) {
                entry.put("response", prior.get(id));
                String priorDec = priorDecision.get(id);
                entry.put("decision", priorDec != null ? priorDec : AskDecision.ACCEPT.getValue());
            } else if (id.equals(newResponder)) {
                entry.put("response", latestResponse != null ? latestResponse : "");
                entry.put("decision", decision != null ? decision : AskDecision.ACCEPT.getValue());
            } else {
                entry.put("response", "");
                entry.put("decision", AskDecision.ACCEPT.getValue());
            }
            list.add(entry);
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialise ask responses: " + e.getMessage(), e);
        }
    }

    /**
     * STG-031: pseudonymize a member's entries in the ask responses ledger —
     * the erasure sweep's ledger surface. The ledger's event shape is kept
     * (response text, decision, and the count a settled quorum recorded), while
     * the responder identity link is severed behind the one-way pseudonym.
     * The member's entries are matched in both the bare and the h:-prefixed
     * stored form, like every other erasure sweep.
     *
     * @param cleanMemberId the member's bare id
     * @param pseudonym     the member's one-way erasure pseudonym
     * @return the number of ask rows rewritten
     */
    @Transactional
    public int pseudonymizeResponderInLedgers(String cleanMemberId, String pseudonym) {
        int rewritten = 0;
        // One pass per stored form; a row matched by both needles is processed
        // once and swept against both forms in that pass.
        java.util.Set<String> processedIds = new java.util.HashSet<>();
        List<String> memberRefs = List.of(cleanMemberId, "h:" + cleanMemberId);
        for (String ref : memberRefs) {
            String needle = "%\"responder\":\"" + ref + "\"%";
            for (Ask ask : askRepository.findByResponsesContaining(needle)) {
                if (!processedIds.add(ask.getId())) continue;
                List<Map<String, String>> entries;
                try {
                    entries = objectMapper.readValue(ask.getResponses(),
                        new TypeReference<List<Map<String, String>>>() {});
                } catch (Exception e) {
                    // One malformed ledger row must not abort the erasure sweep —
                    // name the row and move on, like every other walk sweep.
                    auditService.logSystem("ERASURE_LEDGER_FAIL", "ask", ask.getId(),
                        JsonHelpers.toJson(Map.of("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), objectMapper));
                    continue;
                }
                boolean changed = false;
                for (Map<String, String> entry : entries) {
                    if (entry != null && memberRefs.contains(entry.get("responder"))) {
                        entry.put("responder", pseudonym);
                        changed = true;
                    }
                }
                if (!changed) continue;
                try {
                    ask.setResponses(objectMapper.writeValueAsString(entries));
                    askRepository.save(ask);
                    rewritten++;
                } catch (Exception e) {
                    auditService.logSystem("ERASURE_LEDGER_FAIL", "ask", ask.getId(),
                        JsonHelpers.toJson(Map.of("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), objectMapper));
                }
            }
        }
        return rewritten;
    }

    @Transactional
    public Ask withdraw(String id, String originator) {
        Ask ask = askRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ask not found: " + id));

        if (!ask.isPending()) {
            throw new IllegalStateException("Can only withdraw pending asks, current status: " + ask.getStatus());
        }

        if (JsonHelpers.stripIdPrefix(originator) == null || !JsonHelpers.stripIdPrefix(originator).equals(JsonHelpers.stripIdPrefix(ask.getFrom()))) {
            throw new IllegalArgumentException("Only the originator can withdraw");
        }

        ask.setStatus(AskStatus.WITHDRAWN.getValue());
        Ask saved = askRepository.save(ask);
        auditService.log(originator, "WITHDRAW", "ask", id, null);
        return saved;
    }

    @Transactional
    public Ask expire(String id) {
        Ask ask = askRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ask not found: " + id));

        // ASK-053: an unanswerable quorum ask denies at expiry, fail-safe, "with the
        // breach escalation naming the shortfall" — the audit row names the required
        // quorum, the eligible pool and the gap between them.
        int quorum = ask.getQuorumRequired() != null ? ask.getQuorumRequired() : 1;
        int poolSize = eligiblePoolSize(ask);
        ask.setStatus(AskStatus.EXPIRED.getValue());
        Ask saved = askRepository.save(ask);
        auditService.logSystem("EXPIRE", "ask", id,
            String.format("{\"quorumRequired\":%d,\"eligiblePool\":%d,\"shortfall\":%d,\"behavior\":%s}",
                quorum, poolSize, Math.max(0, quorum - poolSize),
                JsonHelpers.jsonString(ask.getExpiryBehavior() != null ? ask.getExpiryBehavior() : "deny")));
        return saved;
    }

    /**
     * ASK-011: the expiry semantics are explicit per ask — `deny` closes the
     * expired ask, `escalate` and `reassign` close it and open a linked successor
     * ask (up the chain / to the named deputy). The manual door honours the
     * behavior exactly as the scheduled sweep does, so a force-expiry never
     * silently drops the chain.
     *
     * <p>ASK-032: the expiry closure is one of the system's named closures — a
     * member may not force-close an ask through it; an active admin may.
     */
    @Transactional
    public Ask expireNow(String id, String actor) {
        if (!memberService.isAdmin(actor)) {
            throw new IllegalStateException(
                "Only an admin may force-expire an ask — expiry is the system's closure");
        }
        Ask ask = askRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ask not found: " + id));
        if (!ask.isPending()) {
            throw new IllegalStateException("Ask is not pending: " + ask.getStatus());
        }
        String behavior = ask.getExpiryBehavior() != null ? ask.getExpiryBehavior() : "deny";
        if ("escalate".equals(behavior) || "reassign".equals(behavior)) {
            try {
                openExpirySuccessor(ask, behavior, actor);
            } catch (Exception ex) {
                auditService.logSystem("EXPIRE_SUCCESSOR_FAIL", "ask", id,
                    JsonHelpers.toJson(Map.of("behavior", behavior,
                        "error", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()), objectMapper));
            }
        }
        Ask saved = expire(id);
        auditService.log(actor, "EXPIRE", "ask", id,
            String.format("{\"behavior\":%s}", JsonHelpers.jsonString(behavior)));
        return saved;
    }

    /**
     * ASK-011/057/060: the shared successor door an escalating or reassigning expiry
     * opens — one implementation behind both the scheduled sweep and the manual
     * admin door, so the two paths cannot drift.
     *
     * <p>The successor carries the chain's root id in {@code escalation} so depth
     * accumulates across the chain regardless of which door opened the last hop;
     * the depth budget is consulted first — an exhausted chain broadcasts the
     * org-stall alert (ASK-057/058) instead of opening another successor. The deputy
     * hop (ASK-060: member → deputy → domain owner → admin) is walked for both
     * behaviors — `reassign`'s named deputy is the first hop of the same walk —
     * and with none of those hops live the successor lands on the admin broadcast.
     *
     * @return the opened successor, or empty when the chain was exhausted.
     */
    private Optional<Ask> openExpirySuccessor(Ask ask, String behavior, String actor) {
        // Depth is keyed by the root ask ID stored in the escalation field.
        // The check-read-bump must be atomic per chain root: concurrent expiries
        // (scheduled sweep and the manual expireNow door) share successorDepth,
        // so without a per-root lock two threads can read the same depth and blow
        // past MAX_EXPIRE_SUCCESSOR_DEPTH, bypassing the org-stall exhaustion
        // broadcast (ASK-057).
        String rootAskId = ask.getEscalation() != null ? ask.getEscalation() : ask.getId();
        ReentrantLock lock = depthLocks.computeIfAbsent(rootAskId, k -> new ReentrantLock());
        lock.lock();
        try {
            ExpiringEntry<Integer> depthEntry = successorDepth.get(rootAskId);
            int depth = depthEntry != null ? depthEntry.value : 0;
            if (depth >= Defaults.MAX_EXPIRE_SUCCESSOR_DEPTH) {
                // ASK-057: Chain exhausted — broadcast org-stall alert
                broadcastOrgStall(ask);
                auditService.logSystem("EXPIRE_CHAIN_EXHAUSTED", "ask", ask.getId(),
                    String.format("{\"depth\":%d,\"behavior\":\"%s\"}", depth, behavior));
                successorDepth.remove(rootAskId);
                return Optional.empty();
            }
            String successorTo = resolveEscalationTarget(ask);
            // ASK-012/CFG-140: derive deadline from tier defaults
            long deadlineSeconds = deriveDeadlineFromTier(ask.getSlaTier());
            if (deadlineSeconds > Defaults.MAX_DEADLINE_SECONDS) {
                deadlineSeconds = Defaults.MAX_DEADLINE_SECONDS;
            }
            CreatedAsk created = createInternal(ask.getKind(), ask.getFrom(), successorTo, ask.getPayload(),
                ask.getSlaTier(), ask.getExpiryBehavior(), ask.getQuorumRequired(),
                Instant.now().plusSeconds(deadlineSeconds),
                ask.getInitiativeId(), ask.getWorkspaceId(), rootAskId, ask.getId());
            if (created.collapsed()) {
                // ASK-100: an identical pending ask already holds the work; the chain
                // does not spend a depth step on a folded successor. The current ask
                // is still excluded from the collapse candidates, so a successor can
                // never fold back into the very ask that is expiring.
                auditService.log(actor, "EXPIRE_SUCCESSOR_COLLAPSED", "ask", ask.getId(),
                    String.format("{\"canonicalId\":%s,\"behavior\":%s}",
                        JsonHelpers.jsonString(created.ask().getId()), JsonHelpers.jsonString(behavior)));
                return Optional.of(created.ask());
            }
            // Bump the chain depth only when a successor actually lands: the collapse
            // path returns an existing canonical ask without creating a new row,
            // which must not consume depth budget.
            successorDepth.put(rootAskId, new ExpiringEntry<>(depth + 1,
                Instant.now().getEpochSecond() + SUCCESSOR_DEPTH_TTL_SECONDS));
            auditService.log(actor, "EXPIRE_SUCCESSOR_CREATED", "ask", ask.getId(),
                String.format("{\"originalId\":%s,\"behavior\":%s,\"depth\":%d}",
                    JsonHelpers.jsonString(ask.getId()), JsonHelpers.jsonString(behavior), depth + 1));
            return Optional.of(created.ask());
        } finally {
            lock.unlock();
        }
    }

    /**
     * ASK-060: an expiring ask walks member → deputy → domain owner → admin.
     * The domain is the first domain the ask's workspace belongs to (the primary
     * domain for a multi-domain workspace); an ask with no workspace/domain skips
     * that hop, and a non-active or viewer owner contributes nothing. The walk
     * carries a visited set so a mis-configured deputy/owner cycle ends the hop.
     */
    private String resolveEscalationTarget(Ask ask) {
        java.util.Set<String> visited = new java.util.HashSet<>();
        String current = JsonHelpers.stripIdPrefix(ask.getTo());
        if (current != null) visited.add(current);

        Optional<String> deputy = memberService.resolveDeputyTarget(ask.getTo());
        if (deputy.isPresent()) {
            String clean = JsonHelpers.stripIdPrefix(deputy.get());
            if (clean != null && visited.add(clean)) {
                return deputy.get();
            }
        }
        Optional<String> domainOwner = resolveDomainOwnerTarget(ask);
        if (domainOwner.isPresent()) {
            String clean = JsonHelpers.stripIdPrefix(domainOwner.get());
            if (clean != null && visited.add(clean)) {
                return domainOwner.get();
            }
        }
        return OffboardingWalkService.ADMIN_BROADCAST;
    }

    private Optional<String> resolveDomainOwnerTarget(Ask ask) {
        if (ask.getWorkspaceId() == null || ask.getWorkspaceId().isBlank()) return Optional.empty();
        Optional<Workspace> wsOpt = workspaceRepository.findById(ask.getWorkspaceId());
        if (wsOpt.isEmpty()) return Optional.empty();
        String domainIds = wsOpt.get().getDomainIds();
        if (domainIds == null || domainIds.isBlank() || "[]".equals(domainIds)) return Optional.empty();
        try {
            List<String> domains = objectMapper.readValue(domainIds, new TypeReference<List<String>>() {});
            if (domains.isEmpty()) return Optional.empty();
            Optional<DnaDomain> domainOpt = domainRepository.findById(domains.get(0));
            if (domainOpt.isEmpty() || !"active".equals(domainOpt.get().getStatus())) return Optional.empty();
            String owner = domainOpt.get().getOwnerHumanId();
            if (owner == null || owner.isBlank()) return Optional.empty();
            Optional<Human> ownerOpt = memberService.findHuman(JsonHelpers.stripIdPrefix(owner));
            if (ownerOpt.isEmpty() || !ownerOpt.get().isActive()
                    || RbacRole.VIEWER.getValue().equals(ownerOpt.get().getRbac())) {
                return Optional.empty();
            }
            return Optional.of("h:" + ownerOpt.get().getId());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * ASK-051: the eligible pool of an ask is its primary recipient plus every
     * active admin, evaluated at the moment of the settlement. A viewer recipient
     * holds no answerable post and an agent recipient counts toward N > 1 only as
     * a delegated agent (audit-only), so neither joins the pool.
     */
    int eligiblePoolSize(Ask ask) {
        int pool = 0;
        String targetClean = JsonHelpers.stripIdPrefix(ask.getTo());
        if (targetClean != null && !OffboardingWalkService.ADMIN_BROADCAST.equals(targetClean)) {
            Optional<Human> target = memberService.findHuman(targetClean);
            if (target.isPresent() && !RbacRole.VIEWER.getValue().equals(target.get().getRbac())) {
                pool++;
            }
        }
        for (Human admin : memberService.findAdmins()) {
            if (!admin.getId().equals(targetClean)) {
                pool++;
            }
        }
        return pool;
    }

    /**
     * ASK-040: Check if responder is eligible — matches target, deputy, or admin broadcast.
     */
    private boolean isEligibleResponder(Ask ask, String responder) {
        // ASK-031: System cannot respond to its own or others' asks
        String responderClean = JsonHelpers.stripIdPrefix(responder);
        if ("system".equals(responderClean)) {
            return false;
        }

        // Direct match: target is stored as a keyed union (h:<id>/a:<id>) or 'admins'.
        // The responder is normalized the same way, so compare cleaned forms.
        String targetClean = JsonHelpers.stripIdPrefix(ask.getTo());
        if (targetClean != null && targetClean.equals(responderClean)
                && !OffboardingWalkService.ADMIN_BROADCAST.equals(targetClean)) {
            return true;
        }

        // Admin broadcast: any active admin can respond to `admins` target
        if (OffboardingWalkService.ADMIN_BROADCAST.equals(ask.getTo())) {
            return memberService.findAdmins().stream()
                    .anyMatch(h -> h.getId().equals(responderClean));
        }

        // ASK-040/051: for a quorum ask the eligible pool is the pool's primary
        // recipient plus every active admin, evaluated at respond time. An admin
        // is otherwise refused at the door, so an N>1 ask addressed to a single
        // owner could never reach quorum. ASK-056 keeps the broadcast from being
        // ambient authority — the ask stays member-addressed; the admin
        // contributes an approval (ASK-054), it does not re-address the ask.
        int quorum = ask.getQuorumRequired() != null ? ask.getQuorumRequired() : 1;
        if (quorum > 1 && isActiveAdmin(responderClean)) {
            return true;
        }

        // Deputy check: responder is the deputy of the target human
        Optional<Human> targetHuman = memberService.findHuman(targetClean);
        if (targetHuman.isPresent()) {
            String deputyId = targetHuman.get().getDeputyMemberId();
            if (deputyId != null && JsonHelpers.stripIdPrefix(deputyId).equals(responderClean)) {
                return true;
            }
        }

        return false;
    }
}

