package com.summa.service;

import com.summa.repository.DnaProposalRepository;
import com.summa.model.DnaProposal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DnaProposalServiceTest {

    @Mock
    private DnaProposalRepository proposalRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private DnaDomainService domainService;

    @InjectMocks
    private DnaProposalService proposalService;

    @Test
    void create_proposalSetsDefaults() {
        when(proposalRepository.save(any())).thenAnswer(invocation -> {
            DnaProposal p = invocation.getArgument(0);
            if (p.getStatus() == null) p.setStatus("open");
            return p;
        });
        when(domainService.findById("domain-1")).thenReturn(Optional.empty());

        DnaProposal result = proposalService.create("prop-1", "card", "{}", "agent-1", "{}", "domain-1");

        assertNotNull(result);
        assertEquals("open", result.getStatus());
        assertNotNull(result.getReviewBy());
    }

    @Test
    void publish_updatesStatus() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenReturn(proposal);

        DnaProposal result = proposalService.publish("prop-1", "human-1", "human-1");

        assertEquals("published", result.getStatus());
        assertNotNull(result.getReviewedAt());
    }

    @Test
    void publish_throwsWhenNotOpen() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("rejected");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));

        assertThrows(IllegalStateException.class, () -> {
            proposalService.publish("prop-1", "human-1", "human-1");
        });
    }

    @Test
    void withdraw_onlyProposerAllowed() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        proposal.setProposedBy("agent-1");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));

        assertThrows(IllegalArgumentException.class, () -> {
            proposalService.withdraw("prop-1", "agent-2");
        });
    }

    @Test
    void amend_incrementsRevision() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        proposal.setRevision(1);
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenReturn(proposal);

        DnaProposal result = proposalService.amend("prop-1", "{\"new\":\"payload\"}", "agent-1");

        assertEquals(2, result.getRevision());
    }

    @Test
    void amend_throwsWhenNotOpen() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("published");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(proposal));

        assertThrows(IllegalStateException.class, () -> {
            proposalService.amend("prop-1", "{}", "agent-1");
        });
    }
}
