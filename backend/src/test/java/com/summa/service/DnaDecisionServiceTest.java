package com.summa.service;

import com.summa.repository.DnaDecisionRepository;
import com.summa.repository.DnaDomainRepository;
import com.summa.model.DnaDecision;
import com.summa.model.DnaDomain;
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
import com.summa.exception.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class DnaDecisionServiceTest {

    @Mock
    private DnaDecisionRepository decisionRepository;

    @Mock
    private DnaDomainRepository domainRepository;

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
        when(domainRepository.findById("domain-1")).thenReturn(java.util.Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            decisionService.create("d1", "domain-1", "context", "outcome",
                "h:decider", "provenance", "actor"));
    }

    @Test
    void create_throwsWhenDomainNotFound() {
        when(domainRepository.findById("d1")).thenReturn(java.util.Optional.empty());
        assertThrows(EntityNotFoundException.class, () ->
            decisionService.create("d1", "d1", "ctx", "out", "h:decider", "provenance", "actor"));
    }

    @Test
    void create_validDecision_defaultsProvenance() {
        DnaDomain domain = new DnaDomain();
        domain.setId("d1");
        when(domainRepository.findById("d1")).thenReturn(java.util.Optional.of(domain));
        when(decisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DnaDecision result = decisionService.create("d1", "d1", "ctx", "out",
            "h:decider", null, "actor");

        assertNotNull(result);
        assertEquals("d1", result.getDomainId());
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
