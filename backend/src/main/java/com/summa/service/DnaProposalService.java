package com.summa.service;

import com.summa.repository.DnaProposalRepository;
import com.summa.model.DnaProposal;
import com.summa.model.DnaDomain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class DnaProposalService {
    private final DnaProposalRepository proposalRepository;
    private final AuditService auditService;
    private final DnaDomainService domainService;
    private final MemberService memberService;

    private static final Pattern KEYED_UNION_PATTERN = Pattern.compile("^[ha]?:.+$|^[a-zA-Z0-9_-]+$");

    public DnaProposalService(DnaProposalRepository proposalRepository, 
                                AuditService auditService,
                                DnaDomainService domainService,
                                MemberService memberService) {
        this.proposalRepository = proposalRepository;
        this.auditService = auditService;
        this.domainService = domainService;
        this.memberService = memberService;
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
     * DWP-050: Separation of duties — when sod is on for the proposal's domain,
     * the proposer cannot be the publisher. Route publish to the admin broadcast.
     */
    @Transactional
    public DnaProposal publish(String id, String reviewedBy, String actor) {
        DnaProposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + id));
        
        if (!"open".equals(proposal.getStatus())) {
            throw new IllegalStateException("Proposal is not open: " + proposal.getStatus());
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

