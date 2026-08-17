package com.summa.service;

import com.summa.repository.AskRepository;
import com.summa.model.Ask;
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
        // Validate deadline is in the future
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

    public Ask respond(String id, String responder, String response) {
        Ask ask = askRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ask not found: " + id));

        if (!"pending".equals(ask.getStatus())) {
            throw new IllegalStateException("Ask is not pending: " + ask.getStatus());
        }

        // Check eligibility
        if (!isEligibleResponder(ask, responder)) {
            throw new IllegalArgumentException("Responder is not eligible: " + responder);
        }

        // Record response
        ask.setRespondedAt(Instant.now());
        ask.setStatus("answered");

        Ask saved = askRepository.save(ask);
        auditService.log(responder, "RESPOND", "ask", id,
            String.format("{\"response\":\"%s\"}", response != null ? response.substring(0, Math.min(100, response.length())) : ""));
        return saved;
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

    private boolean isEligibleResponder(Ask ask, String responder) {
        // System originator cannot respond
        if ("system".equals(ask.getFrom())) {
            return false;
        }

        // Check if responder matches the target
        return ask.getTo().equals(responder);
    }
}

