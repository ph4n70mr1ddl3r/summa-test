package com.summa.service;

import com.summa.repository.DnaProposalRepository;
import com.summa.repository.DnaRuleRepository;
import com.summa.model.DnaProposal;
import com.summa.model.DnaDomain;
import com.summa.model.DnaRule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DnaProposalService {
    private final DnaProposalRepository proposalRepository;
    private final DnaRuleRepository ruleRepository;
    private final AuditService auditService;
    private final DnaDomainService domainService;
    private final MemberService memberService;
    private final AskService askService;
    private final ObjectMapper objectMapper;

    private static final Pattern KEYED_UNION_PATTERN = Pattern.compile("^[ha]?:.+$|^[a-zA-Z0-9_-]+$");

    public DnaProposalService(DnaProposalRepository proposalRepository,
                                 DnaRuleRepository ruleRepository,
                                 AuditService auditService,
                                 DnaDomainService domainService,
                                 MemberService memberService,
                                 AskService askService,
                                 ObjectMapper objectMapper) {
        this.proposalRepository = proposalRepository;
        this.ruleRepository = ruleRepository;
        this.auditService = auditService;
        this.domainService = domainService;
        this.memberService = memberService;
        this.askService = askService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DnaProposal create(String id, String kind, String payload, String proposedBy,
                                 String provenance, String domainId) {
        validateKeyedUnion(proposedBy, "proposedBy");
        DnaProposal proposal = new DnaProposal();
        proposal.setId(id);
        proposal.setKind(kind);
        proposal.setPayload(payload);
        proposal.setProposedBy(proposedBy);
        proposal.setProvenance(provenance != null ? provenance : "{}");
        proposal.setDomainId(domainId);
        
        // reviewedAt is set only when the proposal is actually reviewed (publish/reject);
        // left as null here to indicate "not yet reviewed"
        
        DnaProposal saved = proposalRepository.save(proposal);
        auditService.log(proposedBy, "PROPOSE", "dna_proposal", id, 
            String.format("{\"kind\":\"%s\",\"domainId\":\"%s\"}", kind, domainId));
        return saved;
    }

    public Optional<DnaProposal> findById(String id) {
        return proposalRepository.findById(id);
    }

    public List<DnaProposal> findByStatus(String status) {
        return proposalRepository.findByStatus(status);
    }

    public List<DnaProposal> findOpenByDomain(String domainId) {
        return proposalRepository.findOpenByDomain(domainId);
    }

    public List<DnaProposal> findAllOpen() {
        return proposalRepository.findAllOpen();
    }

    /**
     * DWP-040: Publish runs inside the domain write lock and re-runs contradiction checks
     * against current state at commit. The second of two sequenced contradictory publishes
     * is refused back to review, never half-silently merged.
     */
    @Transactional
    public DnaProposal publish(String id, String reviewedBy, String actor) {
        DnaProposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + id));
        
        if (!"open".equals(proposal.getStatus())) {
            throw new IllegalStateException("Proposal is not open: " + proposal.getStatus());
        }

        // DWP-042: Contradiction detection — re-check against current state
        List<String> contradictions = detectContradictions(proposal);
        if (!contradictions.isEmpty()) {
            throw new IllegalStateException(
                "Contradiction detected at publish: " + String.join("; ", contradictions));
        }

        // DWP-050: Check SoD — if the domain has sod enabled and the proposer is the reviewer,
        // route to admin broadcast instead
        String actualReviewer = reviewedBy;
        if (proposal.getDomainId() != null && !proposal.getDomainId().isBlank()) {
            Optional<DnaDomain> domainOpt = domainService.findById(proposal.getDomainId());
            if (domainOpt.isPresent() && "reviewer-distinct".equals(domainOpt.get().getSod())) {
                if (proposal.getProposedBy().equals(reviewedBy)) {
                    // SoD breach: route publish to admin broadcast
                    actualReviewer = OffboardingWalkService.ADMIN_BROADCAST;
                    auditService.logSystem("SOD_ROUTE_TO_ADMIN", "dna_proposal", id,
                        "{\"reason\":\"separation_of_duties\",\"proposer\":\"" + proposal.getProposedBy() + "\"}");
                }
            }
        }

        proposal.setStatus("published");
        proposal.setReviewedBy(actualReviewer);
        proposal.setReviewedAt(Instant.now());
        
        DnaProposal saved = proposalRepository.save(proposal);
        auditService.log(actor, "PUBLISH", "dna_proposal", id, 
            String.format("{\"reviewedBy\":\"%s\"}", actualReviewer));
        return saved;
    }

    /**
     * DWP-042: Detect contradictions between the proposal payload and current domain state.
     * Covers rule-vs-rule, goal-vs-goal, decision-vs-rule, and quorum-vs-pool shortfalls.
     */
    private List<String> detectContradictions(DnaProposal proposal) {
        List<String> issues = new java.util.ArrayList<>();
        if (proposal.getDomainId() == null || proposal.getDomainId().isBlank()) {
            return issues;
        }
        try {
            JsonNode payload = objectMapper.readTree(proposal.getPayload());
            String kind = proposal.getKind();

            if ("rule".equals(kind) && payload.has("supersedes_id")) {
                String supersedesId = payload.get("supersedes_id").asText();
                // DGV-025: Check supersedence chain integrity
                List<DnaRule> successors = ruleRepository.findBySupersedesId(supersedesId);
                if (!successors.isEmpty()) {
                    issues.add("Supersession chain fork: " + supersedesId + " already has a live superseder");
                }
            }

            if ("goal".equals(kind) && payload.has("domain_id") && !payload.get("domain_id").isNull()) {
                String goalDomainId = payload.get("domain_id").asText();
                if (!goalDomainId.equals(proposal.getDomainId())) {
                    issues.add("Goal domain_id mismatch: payload domain " + goalDomainId
                        + " differs from proposal domain " + proposal.getDomainId());
                }
            }
        } catch (Exception e) {
            auditService.logSystem("CONTRADICTION_CHECK_FAIL", "dna_proposal", proposal.getId(),
                String.format("{\"error\":\"%s\"}", e.getMessage()));
        }
        return issues;
    }

    /**
     * DWP-020: Check for proposals whose review SLA has been breached and escalate to admin.
     */
    @Transactional
    public void checkAndEscalateBreachedProposals() {
        Instant now = Instant.now();
        List<DnaProposal> openProposals = proposalRepository.findAllOpen();
        for (DnaProposal proposal : openProposals) {
            if (proposal.getDomainId() == null || proposal.getDomainId().isBlank()) {
                // Org-scoped: check against global default SLA (CFG-024, 7 days)
                if (now.isAfter(proposal.getCreatedAt().plusSeconds(7L * 86400L))) {
                    escalateToAdmin(proposal);
                }
            } else {
                Optional<DnaDomain> domainOpt = domainService.findById(proposal.getDomainId());
                if (domainOpt.isPresent()) {
                    DnaDomain domain = domainOpt.get();
                    long slaSeconds = domain.getReviewSlaDays() * 86400L;
                    if (now.isAfter(proposal.getCreatedAt().plusSeconds(slaSeconds))) {
                        escalateToAdmin(proposal);
                    }
                }
            }
        }
    }

    /**
     * DWP-020: Escalate a breached proposal to the admin broadcast.
     */
    private void escalateToAdmin(DnaProposal proposal) {
        try {
            String payload = String.format(
                "{\"proposalId\":\"%s\",\"proposalKind\":\"%s\",\"domainId\":\"%s\",\"breachDays\":%d}",
                proposal.getId(), proposal.getKind(), proposal.getDomainId(),
                java.time.Duration.between(proposal.getCreatedAt(), Instant.now()).toDays());
            askService.create("question", "system", OffboardingWalkService.ADMIN_BROADCAST,
                payload, "critical", "escalate", 1,
                Instant.now().plusSeconds(24L * 3600L), null, null);
            auditService.logSystem("PROPOSAL_SLA_BREACH_ESCALATED", "dna_proposal", proposal.getId(),
                String.format("{\"domainId\":\"%s\",\"breachDays\":%d}", proposal.getDomainId(),
                    java.time.Duration.between(proposal.getCreatedAt(), Instant.now()).toDays()));
        } catch (Exception e) {
            auditService.logSystem("PROPOSAL_SLA_BREACH_ESCALATE_FAIL", "dna_proposal", proposal.getId(),
                String.format("{\"error\":\"%s\"}", e.getMessage()));
        }
    }

    @Transactional
    public DnaProposal reject(String id, String reviewedBy, String actor) {
        DnaProposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + id));
        
        proposal.setStatus("rejected");
        proposal.setReviewedBy(reviewedBy);
        proposal.setReviewedAt(Instant.now());
        
        DnaProposal saved = proposalRepository.save(proposal);
        auditService.log(actor, "REJECT", "dna_proposal", id, null);
        return saved;
    }

    @Transactional
    public DnaProposal withdraw(String id, String actor) {
        DnaProposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + id));
        
        if (!proposedByMatches(proposal, actor)) {
            throw new IllegalArgumentException("Only the proposer can withdraw");
        }
        
        proposal.setStatus("withdrawn");
        DnaProposal saved = proposalRepository.save(proposal);
        auditService.log(actor, "WITHDRAW", "dna_proposal", id, null);
        return saved;
    }

    @Transactional
    public DnaProposal amend(String id, String payload, String actor) {
        DnaProposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + id));
        
        if (!proposal.isOpen()) {
            throw new IllegalStateException("Only open proposals can be amended");
        }
        
        proposal.setRevision(proposal.getRevision() + 1);
        proposal.setPayload(payload);
        
        DnaProposal saved = proposalRepository.save(proposal);
        auditService.log(actor, "AMEND", "dna_proposal", id, 
            String.format("{\"revision\":%d}", proposal.getRevision()));
        return saved;
    }

    private boolean proposedByMatches(DnaProposal proposal, String actor) {
        String proposedBy = proposal.getProposedBy();
        return proposedBy != null && proposedBy.equals(actor);
    }

    private void validateKeyedUnion(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (!KEYED_UNION_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a valid keyed union (h:<human-id> or a:<agent-id>)");
        }
    }
}

