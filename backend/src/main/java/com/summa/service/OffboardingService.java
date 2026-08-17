package com.summa.service;

import com.summa.repository.DnaProposalRepository;
import com.summa.model.DnaProposal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class OffboardingService {
    private final DnaProposalRepository proposalRepository;
    private final AuditService auditService;

    public OffboardingService(DnaProposalRepository proposalRepository, AuditService auditService) {
        this.proposalRepository = proposalRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void transferProposals(String fromMemberId, String toMemberId) {
        List<DnaProposal> proposals = proposalRepository.findByStatus("open");
        for (DnaProposal p : proposals) {
            if (p.getProposedBy().equals(fromMemberId)) {
                p.setProposedBy(toMemberId);
                proposalRepository.save(p);
                auditService.log(toMemberId, "TRANSFER_PROPOSAL", "dna_proposal", p.getId(),
                    String.format("{\"from\":\"%s\",\"to\":\"%s\"}", fromMemberId, toMemberId));
            }
        }
    }

    @Transactional
    public void withdrawProposals(String memberId) {
        List<DnaProposal> proposals = proposalRepository.findByStatus("open");
        for (DnaProposal p : proposals) {
            if (p.getProposedBy().equals(memberId)) {
                p.setStatus("withdrawn");
                proposalRepository.save(p);
                auditService.log("system", "WITHDRAW_ON_OFFBOARD", "dna_proposal", p.getId(),
                    String.format("{\"member\":\"%s\"}", memberId));
            }
        }
    }
}
