package com.summa.service;

import com.summa.repository.DnaDecisionRepository;
import com.summa.repository.DnaDomainRepository;
import com.summa.model.DnaDecision;
import com.summa.model.Human;
import com.summa.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void create_setsDefaults() {
        when(domainRepository.findById("domain-1")).thenReturn(java.util.Optional.of(new com.summa.model.DnaDomain()));
        when(decisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doReturn(List.of()).when(secretsScanner).scan(anyString());

        DnaDecision result = decisionService.create("dec-1", "domain-1", "Context", "Outcome", "h:human-1", null, "admin");

        assertNotNull(result);
        assertEquals("dec-1", result.getId());
        assertEquals("domain-1", result.getDomainId());
        assertEquals("{}", result.getProvenance());
    }

    @Test
    void create_usesProvidedProvenance() {
        when(domainRepository.findById("domain-1")).thenReturn(java.util.Optional.of(new com.summa.model.DnaDomain()));
        when(decisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doReturn(List.of()).when(secretsScanner).scan(anyString());

        DnaDecision result = decisionService.create("dec-1", "domain-1", "ctx", "out", "h:human-1", "my-prov", "admin");

        assertEquals("my-prov", result.getProvenance());
    }

    @Test
    void create_validatesDomainExists() {
        when(domainRepository.findById("domain-1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            decisionService.create("dec-1", "domain-1", "ctx", "out", "h:human-1", "prov", "admin")
        );
    }

    @Test
    void create_skipsDomainValidationWhenBlank() {
        when(decisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doReturn(List.of()).when(secretsScanner).scan(anyString());

        DnaDecision result = decisionService.create("dec-1", "", "ctx", "out", "h:human-1", "prov", "admin");

        assertNotNull(result);
    }

    @Test
    void create_stripsIdPrefixFromDecidedBy() {
        when(decisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doReturn(List.of()).when(secretsScanner).scan(anyString());

        DnaDecision result = decisionService.create("dec-1", null, "ctx", "out", "h:human-1", "prov", "admin");

        assertEquals("human-1", result.getDecidedBy());
    }

    @Test
    void findById_returnsPresent() {
        DnaDecision dec = new DnaDecision();
        dec.setId("dec-1");
        when(decisionRepository.findById("dec-1")).thenReturn(Optional.of(dec));

        Optional<DnaDecision> result = decisionService.findById("dec-1");

        assertTrue(result.isPresent());
        assertEquals("dec-1", result.get().getId());
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(decisionRepository.findById("dec-1")).thenReturn(Optional.empty());

        Optional<DnaDecision> result = decisionService.findById("dec-1");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByDomain_returnsDecisions() {
        DnaDecision dec = new DnaDecision();
        dec.setId("dec-1");
        when(decisionRepository.findByDomainId("domain-1")).thenReturn(List.of(dec));

        List<DnaDecision> results = decisionService.findByDomain("domain-1");

        assertEquals(1, results.size());
        assertEquals("dec-1", results.get(0).getId());
    }

    @Test
    void findAll_returnsAll() {
        DnaDecision dec = new DnaDecision();
        dec.setId("dec-1");
        when(decisionRepository.findAll()).thenReturn(List.of(dec));

        List<DnaDecision> results = decisionService.findAll();

        assertEquals(1, results.size());
    }
}
