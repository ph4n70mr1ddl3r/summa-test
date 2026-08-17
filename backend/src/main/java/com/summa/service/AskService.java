package com.summa.service;

import com.summa.repository.AskRepository;
import com.summa.model.Ask;
import com.summa.model.Human;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Service
public class AskService {
    private final AskRepository askRepository;
    private final AuditService auditService;
    private final MemberService memberService;
    private final long stormCollapseWindowSeconds;

    // ASK-100: Storm collapse window — tracks recent ask creation by (kind, to, payloadHash)
    private final ConcurrentHashMap<String, Instant> collapseWindowTimestamps = new ConcurrentHashMap<>();

    public AskService(AskRepository askRepository, AuditService auditService, MemberService memberService,
                      @Value("${summa.asks.storm-collapse-window-hours:1}") long stormCollapseWindowHours) {
        this.askRepository = askRepository;
        this.auditService = auditService;
        this.memberService = memberService;
        this.stormCollapseWindowSeconds = stormCollapseWindowHours * 3600L;
    }

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

        // ASK-100: Storm collapse — if identical pending ask exists within collapse window,
        // attach to canonical instead of creating duplicate
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
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return kind + "|" + to + "|" + java.util.Objects.hash(payload);
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
        Ask saved = askRepository.save(ask);

        // Check if quorum reached
        if (existingResponses.size() >= ask.getQuorumRequired()) {
            saved.setStatus("answered");
            saved.setRespondedAt(Instant.now());
            saved = askRepository.save(saved);
        }

        auditService.log(responder, "RESPOND", "ask", id,
            String.format("{\"response\":\"%s\",\"quorumProgress\":%d/%d}",
                response != null ? response.substring(0, Math.min(100, response.length())) : "",
                existingResponses.size(), ask.getQuorumRequired()));
        return saved;
    }

    private void recordResponse(Ask ask, String responder, String response) {
        List<String> ids = parseResponseIds(ask);
        ids.add(responder);
        ask.setResponses(toJsonResponseList(ids, response));
    }

    private List<String> parseResponseIds(Ask ask) {
        try {
            String resp = ask.getResponses();
            if (resp == null || resp.isBlank() || resp.equals("[]")) return new java.util.ArrayList<>();
            return new java.util.ArrayList<>(java.util.List.of(resp.replaceAll("[\\[\\]]", "").split(",")));
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    private String toJsonResponseList(List<String> ids, String response) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(ids.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
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
        if ("admins".equals(ask.getTo())) {
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

