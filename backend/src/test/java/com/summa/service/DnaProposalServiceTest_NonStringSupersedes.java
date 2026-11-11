package com.summa.service;

import com.summa.repository.DnaProposalRepository;
import com.summa.repository.DnaRuleRepository;
import com.summa.model.DnaProposal;
import com.summa.model.DnaRule;
import com.summa.model.Human;
import com.summa.model.Agent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DnaProposalServiceTest_NonStringSupersedes {

    @Mock
    private DnaProposalRepository proposalRepository;

    @Mock
    private DnaRuleRepository ruleRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private DnaDomainService domainService;

    @Mock
    private MemberService memberService;

    @Mock
    private AskService askService;

    @Mock
    private GovernanceService governanceService;

    @Mock
    private SecretsScanner secretsScanner;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private DnaProposalService proposalService;

    @BeforeEach
    void setUp() {
        proposalService = new DnaProposalService(
            proposalRepository, ruleRepository, auditService,
            domainService, memberService, askService, governanceService, objectMapper,
            secretsScanner, new DomainLockService()
        );
    }

    @Test
    void detectContradictions_ruleSupersedesIdNonStringShouldNotProduceContradiction() {
        // Bug hunt: detectContradictions() checks supersedes_id without verifying it's a string.
        // If supersedes_id is a non-string JSON value (number, boolean), asText()
        // returns an unreliable representation, potentially causing false contradictions.
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        proposal.setKind("rule");
        // Payload with supersedes_id as a non-string JSON value (number)
        proposal.setPayload("{\"supersedes_id\": 42}");

        List<String> contradictions = proposalService.detectContradictions(proposal);

        // Non-string supersedes_id should not produce a false contradiction
        assertTrue(contradictions.isEmpty(),
            "Expected no contradictions for non-string supersedes_id, but got: " + contradictions);
    }

    @Test
    void detectContradictions_ruleSupersedesIdBooleanShouldNotProduceContradiction() {
        // Same as above, but with a boolean supersedes_id value.
        DnaProposal proposal = new DnaProposal();
        proposal.setId("prop-1");
        proposal.setStatus("open");
        proposal.setKind("rule");
        // Payload with supersedes_id as a boolean
        proposal.setPayload("{\"supersedes_id\": true}");

        List<String> contradictions = proposalService.detectContradictions(proposal);

        // Non-string supersedes_id should not produce a false contradiction
        assertTrue(contradictions.isEmpty(),
            "Expected no contradictions for boolean supersedes_id, but got: " + contradictions);
    }
}
