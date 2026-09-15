package com.summa.service;

import com.summa.repository.DnaDecisionRepository;
import com.summa.model.DnaDecision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DnaDecisionServiceTest {

    @Mock
    private DnaDecisionRepository decisionRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private MemberService memberService;

    @Mock
    private SecretsScanner secretsScanner;

    @InjectMocks
    private DnaDecisionService decisionService;

    @Test
    void create_validDecision() {
        DnaDecision decision = new DnaDecision();
        decision.setId("d1");
        when(decisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DnaDecision result = decisionService.create("d1", "domain-1", "context", "outcome",
            "h:decider", "provenance", "actor");

        assertNotNull(result);
        assertEquals("h:decider", result.getDecidedBy());
        verify(auditService).log(eq("actor"), eq("CREATE_DECISION"), eq("dna_decision"), eq("d1"), anyString());
    }

    @Test
    void create_throwsWhenNullDecidedBy() {
        assertThrows(IllegalArgumentException.class, () ->
            decisionService.create("d1", "d1", "ctx", "out", null, null, "actor"));
    }

    @Test
    void create_throwsWhenBlankDecidedBy() {
        assertThrows(IllegalArgumentException.class, () ->
            decisionService.create("d1", "d1", "ctx", "out", "   ", null, "actor"));
    }

    @Test
    void create_throwsWhenInvalidKeyedUnion() {
        assertThrows(IllegalArgumentException.class, () ->
            decisionService.create("d1", "d1", "ctx", "out", "bad format!", null, "actor"));
    }

    @Test
    void create_defaultsProvenance() {
        DnaDecision decision = new DnaDecision();
        decision.setId("d1");
        when(decisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DnaDecision result = decisionService.create("d1", "d1", "ctx", "out", "h:user", null, "actor");

        assertEquals("{}", result.getProvenance());
    }

    @Test
    void findById_returnsPresent() {
        DnaDecision decision = new DnaDecision();
        decision.setId("d1");
        when(decisionRepository.findById("d1")).thenReturn(Optional.of(decision));

        Optional<DnaDecision> result = decisionService.findById("d1");

        assertTrue(result.isPresent());
    }

    @Test
    void findByDomain_returnsList() {
        DnaDecision d = new DnaDecision();
        d.setId("d1");
        when(decisionRepository.findByDomainId("domain-1")).thenReturn(List.of(d));

        List<DnaDecision> result = decisionService.findByDomain("domain-1");

        assertEquals(1, result.size());
    }
}
