package com.summa.service;

import com.summa.repository.DnaProposalRepository;
import com.summa.model.DnaProposal;
import com.summa.model.DnaDomain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DnaProposalService {
    private final DnaProposalRepository proposalRepository;
    private final AuditService auditService;
    private final DnaDomainService domainService;

    public DnaProposalService(DnaProposalRepository proposalRepository, 
                               AuditService auditService,
                               DnaDomainService domainService) {
        this.proposalRepository = proposalRepository;
        this.auditService = auditService;
        this.domainService = domainService;
    }

    public DnaProposal create(String id, String kind, String payload, String proposedBy, 
                               String provenance, String domainId) {
        DnaProposal proposal = new DnaProposal();
        proposal.setId(id);
        proposal.setKind(kind);
        proposal.setPayload(payload);
        proposal.setProposedBy(proposedBy);
        proposal.setProvenance(provenance != null ? provenance : "{}");
        proposal.setDomainId(domainId);
        
        // Set review_by based on domain
        if (domainId != null) {
            Optional<DnaDomain> domainOpt = domainService.findById(domainId);
            if (domainOpt.isPresent()) {
                proposal.setReviewBy(Instant.now().plusSeconds(domainOpt.get().getReviewSlaDays() * 86400L));
            } else {
                // Domain not found — use default 7 days
                proposal.setReviewBy(Instant.now().plusSeconds(7 * 86400L));
            }
        } else {
            // Org-scoped: use default 7 days
            proposal.setReviewBy(Instant.now().plusSeconds(7 * 86400L));
        }
        
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

    @Transactional
    public DnaProposal publish(String id, String reviewedBy, String actor) {
        DnaProposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found: " + id));
        
        if (!"open".equals(proposal.getStatus())) {
            throw new IllegalStateException("Proposal is not open: " + proposal.getStatus());
        }
        
        proposal.setStatus("published");
        proposal.setReviewedBy(reviewedBy);
        proposal.setReviewedAt(Instant.now());
        
        DnaProposal saved = proposalRepository.save(proposal);
        auditService.log(actor, "PUBLISH", "dna_proposal", id, 
            String.format("{\"reviewedBy\":\"%s\"}", reviewedBy));
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
        return proposal.getProposedBy().equals(actor);
    }
}
