package com.summa.service;

import com.summa.repository.AskRepository;
import com.summa.model.Ask;
import com.summa.model.Human;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Base64;
import java.util.Objects;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class AskService {
    private static final long DEFAULT_CRITICAL_ASK_DEADLINE_HOURS = 1;
    private static final long DEFAULT_BULK_ASK_DEADLINE_HOURS = 24;
    private static final long DEFAULT_STANDARD_ASK_DEADLINE_HOURS = 24;

    private final AskRepository askRepository;
    private final AuditService auditService;
    private final MemberService memberService;
    private final GovernanceService governanceService;
    private final long stormCollapseWindowSeconds;

    private static final int MAX_EXPIRE_SUCCESSOR_DEPTH = 5;

    // ASK-100: Storm collapse window — tracks recent ask creation by (kind, to, payloadHash)
    private final ConcurrentHashMap<String, Instant> collapseWindowTimestamps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> successorDepth = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public AskService(AskRepository askRepository, AuditService auditService, MemberService memberService,
                      GovernanceService governanceService,
                      @Value("${summa.asks.storm-collapse-window-hours:1}") long stormCollapseWindowHours,
                      ObjectMapper objectMapper) {
        this.askRepository = askRepository;
        this.auditService = auditService;
        this.memberService = memberService;
        this.governanceService = governanceService;
        this.stormCollapseWindowSeconds = stormCollapseWindowHours * 3600L;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Ask create(String kind, String from, String to, String payload, String slaTier,
                      String expiryBehavior, Integer quorumRequired, Instant deadline,
                      String initiativeId, String workspaceId) {
        // ASK-012: explicit deadline earlier than creation is refused
        if (deadline.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Deadline must be in the future");
        }

        Ask ask = new Ask();
        ask.setId(UUID.randomUUID().toString());
        ask.setKind(kind);
        ask.setFrom(from);
        ask.setTo(to);
        ask.setPayload(payload != null ? payload : "{}");
        ask.setSlaTier(slaTier != null ? slaTier : "standard");
        ask.setExpiryBehavior(expiryBehavior != null ? expiryBehavior : "deny");
        ask.setQuorumRequired(quorumRequired != null ? quorumRequired : 1);
        ask.setDeadline(deadline);
        ask.setInitiativeId(initiativeId);
        ask.setWorkspaceId(workspaceId);

        // ASK-100: Storm collapse — identical pending asks (same kind, target, payload hash)
        // collapse into one canonical ask regardless of originator (ASK-033: retraction is
        // originator-scoped, but the collapse key is communal).
        String collapseKey = buildCollapseKey(kind, to, payload);
        Instant now = Instant.now();
        Instant lastCreated = collapseWindowTimestamps.get(collapseKey);
        if (lastCreated != null && now.getEpochSecond() - lastCreated.getEpochSecond() < stormCollapseWindowSeconds) {
            // Collapse: increment collapsed_count on nearest pending canonical
            List<Ask> candidates = findPendingByKindAndTo(kind, to);
            if (!candidates.isEmpty()) {
                Ask canonical = candidates.get(0);
                canonical.setCollapsedCount(canonical.getCollapsedCount() + 1);
                Ask saved = askRepository.save(canonical);
                auditService.log(from, "COLLAPSED_ASK", "ask", saved.getId(),
                    String.format("{\"newAskId\":\"%s\",\"collapsedCount\":%d}", ask.getId(), saved.getCollapsedCount()));
                return saved;
            }
        }
        collapseWindowTimestamps.put(collapseKey, now);

        Ask saved = askRepository.save(ask);
        auditService.log(from, "CREATE", "ask", ask.getId(),
            String.format("{\"kind\":\"%s\",\"to\":\"%s\",\"tier\":\"%s\"}", kind, to, ask.getSlaTier()));
        return saved;
    }

    private String buildCollapseKey(String kind, String to, String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((kind + "|" + to + "|" + (payload != null ? payload : "")).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return kind + "|" + to + "|" + Objects.hash(payload);
        }
    }

    private List<Ask> findPendingByKindAndTo(String kind, String to) {
        return askRepository.findByToAndStatusPending(to).stream()
                .filter(a -> kind.equals(a.getKind()) && "pending".equals(a.getStatus()))
                .toList();
    }

    public Optional<Ask> findById(String id) {
        return askRepository.findById(id);
    }

    public List<Ask> findByTo(String to) {
        return askRepository.findByToAndStatusPending(to);
    }

    public List<Ask> findByStatus(String status) {
        return askRepository.findByStatus(status);
    }

    public List<Ask> findAllPending() {
        return askRepository.findByStatus("pending");
    }

    public List<Ask> findExpired() {
        return askRepository.findExpiredBefore(Instant.now());
    }

    /**
     * ASK-011: Periodically expire asks whose deadline has passed.
     * Processes each expired ask according to its expiry_behavior:
     * - deny: closes as expired
     * - escalate: closes as expired, files successor to escalation target
     * - reassign: closes as expired, files successor to deputy/admin
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processExpiredAsks() {
        List<Ask> expired = askRepository.findExpiredBefore(Instant.now());
        for (Ask ask : expired) {
            if (!"pending".equals(ask.getStatus())) continue;
            String behavior = ask.getExpiryBehavior() != null ? ask.getExpiryBehavior() : "deny";
            if ("deny".equals(behavior)) {
                expire(ask.getId());
            } else if ("escalate".equals(behavior) || "reassign".equals(behavior)) {
                expire(ask.getId());
                try {
                    int depth = successorDepth.getOrDefault(ask.getId(), 0) + 1;
                    if (depth > MAX_EXPIRE_SUCCESSOR_DEPTH) {
                        auditService.logSystem("EXPIRE_MAX_DEPTH_REACHED", "ask", ask.getId(),
                            String.format("{\"depth\":%d,\"behavior\":\"%s\"}", depth, behavior));
                        continue;
                    }
                    String successorTo = OffboardingWalkService.ADMIN_BROADCAST;
                    Optional<Human> target = memberService.findHuman(ask.getTo());
                    if (target.isPresent() && target.get().getDeputyMemberId() != null) {
                        successorTo = target.get().getDeputyMemberId();
                    }
                    // ASK-012/CFG-140: derive deadline from tier defaults
                    long successorDeadlineSeconds = deriveDeadlineFromTier(ask.getSlaTier());
                    Ask successor = create(ask.getKind(), ask.getFrom(), successorTo,
                        ask.getPayload(), ask.getSlaTier(), "deny",
                        ask.getQuorumRequired(),
                        Instant.now().plusSeconds(successorDeadlineSeconds),
                        ask.getInitiativeId(), ask.getWorkspaceId());
                    successorDepth.put(successor.getId(), depth);
                    auditService.logSystem("EXPIRE_SUCCESSOR_CREATED", "ask", successor.getId(),
                        String.format("{\"originalId\":\"%s\",\"behavior\":\"%s\",\"depth\":%d}", ask.getId(), behavior, depth));
                } catch (Exception e) {
                    auditService.logSystem("EXPIRE_SUCCESSOR_FAIL", "ask", ask.getId(),
                        String.format("{\"behavior\":\"%s\",\"error\":\"%s\"}", behavior, e.getMessage()));
                }
            }
        }
    }

    /**
     * ASK-015/040/050: Respond to an ask with eligibility, quorum, and re-validation checks.
     */
    public Ask respond(String id, String responder, String response) {
        Ask ask = askRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ask not found: " + id));

        if (!"pending".equals(ask.getStatus())) {
            throw new IllegalStateException("Ask is not pending: " + ask.getStatus());
        }

        // ASK-040: Check eligibility at the door
        if (!isEligibleResponder(ask, responder)) {
            throw new IllegalArgumentException("Responder is not eligible: " + responder);
        }

        // ASK-015: quorum-1 closes on first response; later responses are audit-only
        int quorum = ask.getQuorumRequired() != null ? ask.getQuorumRequired() : 1;
        if (quorum == 1) {
            recordResponse(ask, responder, response);
            ask.setRespondedAt(Instant.now());
            ask.setStatus("answered");
            Ask saved = askRepository.save(ask);
            auditService.log(responder, "RESPOND", "ask", id,
                String.format("{\"response\":\"%s\"}", response != null ? response.substring(0, Math.min(100, response.length())) : ""));
            return saved;
        }

        // ASK-050: N > 1 quorum — collect responses until N distinct principals answered
        List<String> existingResponses = parseResponseIds(ask);
        if (existingResponses.contains(responder)) {
            // Already responded — audit-only duplicate
            auditService.logSystem("AUDIT_ONLY_RESPONSE", "ask", id,
                String.format("{\"responder\":\"%s\"}", responder));
            return ask;
        }

        existingResponses.add(responder);
        ask.setResponses(toJsonResponseList(existingResponses, response));
        boolean quorumReached = existingResponses.size() >= ask.getQuorumRequired();
        if (quorumReached) {
            ask.setStatus("answered");
            ask.setRespondedAt(Instant.now());
        }
        Ask saved = askRepository.save(ask);

        auditService.log(responder, "RESPOND", "ask", id,
            String.format("{\"response\":\"%s\",\"quorumProgress\":%d/%d}",
                response != null ? response.substring(0, Math.min(100, response.length())) : "",
                existingResponses.size(), ask.getQuorumRequired()));
        return saved;
    }

    /**
     * ASK-012/CFG-140: Derive deadline seconds from SLA tier defaults.
     */
    private long deriveDeadlineFromTier(String tier) {
        if ("critical".equals(tier)) {
            Object val = governanceService.getSetting("asks-tier-critical-deadline-hours");
            if (val instanceof Number) return ((Number) val).longValue() * 3600L;
            return DEFAULT_CRITICAL_ASK_DEADLINE_HOURS * 3600L;
        }
        if ("bulk".equals(tier)) {
            Object val = governanceService.getSetting("asks-tier-bulk-deadline-hours");
            if (val instanceof Number) return ((Number) val).longValue() * 3600L;
            return DEFAULT_BULK_ASK_DEADLINE_HOURS * 3600L;
        }
        // standard tier: next digest is not a fixed deadline — use 24h as safe upper bound
        return DEFAULT_STANDARD_ASK_DEADLINE_HOURS * 3600L;
    }

    private void recordResponse(Ask ask, String responder, String response) {
        List<String> ids = parseResponseIds(ask);
        ids.add(responder);
        ask.setResponses(toJsonResponseList(ids, response));
    }

    private List<String> parseResponseIds(Ask ask) {
        try {
            String resp = ask.getResponses();
            if (resp == null || resp.isBlank() || resp.equals("[]")) return new ArrayList<>();
            return objectMapper.readValue(resp, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJsonResponseList(List<String> ids, String response) {
        try {
            List<Map<String, String>> list = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                Map<String, String> entry = new HashMap<>();
                entry.put("responder", ids.get(i));
                if (i == ids.size() - 1 && response != null) {
                    entry.put("response", response);
                }
                list.add(entry);
            }
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < ids.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(ids.get(i)).append("\"");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    public Ask withdraw(String id, String originator) {
        Ask ask = askRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ask not found: " + id));

        if (!ask.getFrom().equals(originator)) {
            throw new IllegalArgumentException("Only the originator can withdraw");
        }

        ask.setStatus("withdrawn");
        Ask saved = askRepository.save(ask);
        auditService.log(originator, "WITHDRAW", "ask", id, null);
        return saved;
    }

    public Ask expire(String id) {
        Ask ask = askRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ask not found: " + id));

        ask.setStatus("expired");
        Ask saved = askRepository.save(ask);
        auditService.logSystem("EXPIRE", "ask", id, null);
        return saved;
    }

    /**
     * ASK-040: Check if responder is eligible — matches target, deputy, or admin broadcast.
     */
    private boolean isEligibleResponder(Ask ask, String responder) {
        // System originator cannot respond (ASK-031)
        if ("system".equals(ask.getFrom())) {
            return false;
        }

        // Direct match
        if (ask.getTo().equals(responder)) {
            return true;
        }

        // Admin broadcast: any active admin can respond to `admins` target
        if (OffboardingWalkService.ADMIN_BROADCAST.equals(ask.getTo())) {
            return memberService.findAdmins().stream()
                    .anyMatch(h -> h.getId().equals(responder));
        }

        // Deputy check: responder is the deputy of the target human
        Optional<Human> targetHuman = memberService.findHuman(ask.getTo());
        if (targetHuman.isPresent()) {
            String deputyId = targetHuman.get().getDeputyMemberId();
            if (deputyId != null && deputyId.equals(responder)) {
                return true;
            }
        }

        return false;
    }
}

