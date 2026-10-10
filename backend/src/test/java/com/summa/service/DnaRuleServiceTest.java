package com.summa.service;

import com.summa.repository.DnaRuleRepository;
import com.summa.model.DnaRule;
import com.summa.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DnaRuleServiceTest {

    @Mock
    private DnaRuleRepository ruleRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private DnaDomainService domainService;

    @Mock
    private SecretsScanner secretsScanner;

    @InjectMocks
    private DnaRuleService ruleService;

    @Test
    void create_setsDefaults() {
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doReturn(List.of()).when(secretsScanner).scan(anyString());

        DnaRule result = ruleService.create("rule-1", "domain-1", "Statement", "hint", null, null, null, "admin");

        assertNotNull(result);
        assertEquals("rule-1", result.getId());
        assertEquals("domain-1", result.getDomainId());
        assertEquals("active", result.getStatus());
        assertEquals("hint", result.getMachineHint());
    }

    @Test
    void create_setsLapsedWhenEffectiveToInPast() {
        Instant past = Instant.now().minusSeconds(86400);
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doReturn(List.of()).when(secretsScanner).scan(anyString());

        DnaRule result = ruleService.create("rule-1", "domain-1", "stmt", null, null, past, null, "admin");

        assertEquals("lapsed", result.getStatus());
    }

    @Test
    void create_rejectsToBeforeFrom() {
        Instant from = Instant.now();
        Instant to = Instant.now().minusSeconds(86400);

        assertThrows(IllegalArgumentException.class, () ->
            ruleService.create("rule-1", "domain-1", "stmt", null, from, to, null, "admin")
        );
    }

    @Test
    void create_validatesSupersedesDomain() {
        DnaRule existing = new DnaRule();
        existing.setId("rule-old");
        existing.setDomainId("other-domain");
        existing.setStatus("active");
        lenient().when(ruleRepository.findById("rule-old")).thenReturn(Optional.of(existing));
        lenient().when(ruleRepository.findBySupersedesId("rule-old")).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () ->
            ruleService.create("rule-1", "domain-1", "stmt", null, null, null, "rule-old", "admin")
        );
    }

    @Test
    void create_rejectsForkedSupersession() {
        DnaRule existing = new DnaRule();
        existing.setId("rule-old");
        existing.setDomainId("domain-1");
        existing.setStatus("active");
        when(ruleRepository.findById("rule-old")).thenReturn(Optional.of(existing));
        when(ruleRepository.findBySupersedesId("rule-old")).thenReturn(List.of(new DnaRule()));

        assertThrows(IllegalArgumentException.class, () ->
            ruleService.create("rule-1", "domain-1", "stmt", null, null, null, "rule-old", "admin")
        );
    }

    @Test
    void create_allowsMissingSupersedesAsForwardReference() {
        when(ruleRepository.findById("rule-future")).thenReturn(Optional.empty());
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doReturn(List.of()).when(secretsScanner).scan(anyString());

        DnaRule result = ruleService.create("rule-1", "domain-1", "stmt", null, null, null, "rule-future", "admin");

        assertEquals("rule-future", result.getSupersedesId());
    }

    @Test
    void findById_returnsPresent() {
        DnaRule rule = new DnaRule();
        rule.setId("rule-1");
        when(ruleRepository.findById("rule-1")).thenReturn(Optional.of(rule));

        Optional<DnaRule> result = ruleService.findById("rule-1");

        assertTrue(result.isPresent());
    }

    @Test
    void findByDomain_returnsRules() {
        when(ruleRepository.findByDomainId("domain-1")).thenReturn(List.of());

        List<DnaRule> results = ruleService.findByDomain("domain-1");

        assertNotNull(results);
        verify(ruleRepository).findByDomainId("domain-1");
    }

    @Test
    void update_allowsActiveRule() {
        DnaRule rule = new DnaRule();
        rule.setId("rule-1");
        rule.setStatus("active");
        rule.setDomainId("domain-1");
        when(ruleRepository.findById("rule-1")).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaRule result = ruleService.update("rule-1", "New statement", null, null, "admin");

        assertEquals("New statement", result.getStatementMd());
    }

    @Test
    void update_rejectsNonActiveRule() {
        DnaRule rule = new DnaRule();
        rule.setId("rule-1");
        rule.setStatus("lapsed");
        when(ruleRepository.findById("rule-1")).thenReturn(Optional.of(rule));

        assertThrows(IllegalArgumentException.class, () ->
            ruleService.update("rule-1", "New statement", null, null, "admin")
        );
    }

    @Test
    void update_throwsWhenNotFound() {
        when(ruleRepository.findById("rule-1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            ruleService.update("rule-1", "New statement", null, null, "admin")
        );
    }

    @Test
    void update_setsLapsedWhenEffectiveToInPast() {
        DnaRule rule = new DnaRule();
        rule.setId("rule-1");
        rule.setStatus("active");
        rule.setDomainId("domain-1");
        when(ruleRepository.findById("rule-1")).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Instant past = Instant.now().minusSeconds(86400);
        DnaRule result = ruleService.update("rule-1", null, null, past, "admin");

        assertEquals("lapsed", result.getStatus());
    }

    @Test
    void supersede_updatesPredecessorAndSuccessor() {
        DnaRule predecessor = new DnaRule();
        predecessor.setId("rule-old");
        predecessor.setDomainId("domain-1");
        predecessor.setStatus("active");
        DnaRule successor = new DnaRule();
        successor.setId("rule-new");
        successor.setDomainId("domain-1");
        successor.setStatus("active");
        when(ruleRepository.findById("rule-new")).thenReturn(Optional.of(successor));
        when(ruleRepository.findById("rule-old")).thenReturn(Optional.of(predecessor));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaRule result = ruleService.supersede("rule-new", "rule-old", "admin");

        assertEquals("rule-old", successor.getSupersedesId());
        assertEquals("superseded", predecessor.getStatus());
        assertEquals("rule-old", result.getId());
    }

    @Test
    void supersede_throwsWhenPredecessorNotFound() {
        DnaRule successor = new DnaRule();
        successor.setId("rule-new");
        successor.setDomainId("domain-1");
        when(ruleRepository.findById("rule-new")).thenReturn(Optional.of(successor));
        when(ruleRepository.findById("rule-old")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            ruleService.supersede("rule-new", "rule-old", "admin")
        );
    }

    @Test
    void supersede_throwsOnCrossDomain() {
        DnaRule predecessor = new DnaRule();
        predecessor.setId("rule-old");
        predecessor.setDomainId("other-domain");
        predecessor.setStatus("active");
        DnaRule successor = new DnaRule();
        successor.setId("rule-new");
        successor.setDomainId("domain-1");
        successor.setStatus("active");
        when(ruleRepository.findById("rule-new")).thenReturn(Optional.of(successor));
        when(ruleRepository.findById("rule-old")).thenReturn(Optional.of(predecessor));

        assertThrows(IllegalArgumentException.class, () ->
            ruleService.supersede("rule-new", "rule-old", "admin")
        );
    }
}
