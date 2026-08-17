package com.summa.service;

import com.summa.repository.AskRepository;
import com.summa.model.Ask;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AskService {
    private final AskRepository askRepository;
    private final AuditService auditService;
    private final MemberService memberService;

    public AskService(AskRepository askRepository, AuditService auditService, MemberService memberService) {
        this.askRepository = askRepository;
        this.auditService = auditService;
        this.memberService = memberService;
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

        Ask saved = askRepository.save(ask);
        auditService.log(from, "CREATE", "ask", ask.getId(), 
            String.format("{\"kind\":\"%s\",\"to\":\"%s\",\"tier\":\"%s\"}", kind, to, ask.getSlaTier()));
        return saved;
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

        if (!ask.getFrom().equals(originator) && !"system".equals(originator)) {
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
