package com.summa.service;

import com.summa.repository.DnaRuleRepository;
import com.summa.model.DnaRule;
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
import com.summa.exception.EntityNotFoundException;

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
    void create_ruleWithDefaults() {
        DnaRule rule = new DnaRule();
        rule.setId("rule-1");
        rule.setDomainId("domain-1");
        rule.setStatus("active");
        when(ruleRepository.save(any())).thenReturn(rule);

        DnaRule result = ruleService.create("rule-1", "domain-1", "Statement", null,
            Instant.now(), null, null, "actor");

        assertNotNull(result);
        assertEquals("active", result.getStatus());
    }

    @Test
    void update_throwsWhenRuleNotActive() {
        DnaRule rule = new DnaRule();
        rule.setId("rule-1");
        rule.setStatus("superseded");
        when(ruleRepository.findById("rule-1")).thenReturn(Optional.of(rule));

        assertThrows(IllegalArgumentException.class, () -> {
            ruleService.update("rule-1", null, null, null, "actor");
        });
    }

    @Test
    void create_throwsWhenEffectiveToBeforeEffectiveFrom() {
        Instant from = Instant.now().plusSeconds(3600);
        Instant to = Instant.now().minusSeconds(3600);

        assertThrows(IllegalArgumentException.class, () -> {
            ruleService.create("rule-1", "domain-1", "Statement", null, from, to, null, "actor");
        });
    }

    @Test
    void create_throwsWhenSupersedesNotFoundButExistsWithWrongDomain() {
        DnaRule existing = new DnaRule();
        existing.setId("rule-old");
        existing.setDomainId("domain-other");
        lenient().when(ruleRepository.findById("rule-old")).thenReturn(Optional.of(existing));
        lenient().when(ruleRepository.findBySupersedesId("rule-old")).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> {
            ruleService.create("rule-1", "domain-1", "Statement", null,
                Instant.now(), null, "rule-old", "actor");
        });
    }

    @Test
    void create_throwsWhenSupersedesAlreadyHasSuccessor() {
        DnaRule existing = new DnaRule();
        existing.setId("rule-old");
        existing.setDomainId("domain-1");
        existing.setStatus("active");
        when(ruleRepository.findById("rule-old")).thenReturn(Optional.of(existing));
        when(ruleRepository.findBySupersedesId("rule-old")).thenReturn(List.of(new DnaRule()));

        assertThrows(IllegalArgumentException.class, () -> {
            ruleService.create("rule-1", "domain-1", "Statement", null,
                Instant.now(), null, "rule-old", "actor");
        });
    }

    @Test
    void supersede_validSupersedes() {
        DnaRule successor = new DnaRule();
        successor.setId("rule-new");
        successor.setDomainId("domain-1");
        DnaRule predecessor = new DnaRule();
        predecessor.setId("rule-old");
        predecessor.setDomainId("domain-1");
        predecessor.setStatus("active");
        when(ruleRepository.findById("rule-new")).thenReturn(Optional.of(successor));
        when(ruleRepository.findById("rule-old")).thenReturn(Optional.of(predecessor));
        when(ruleRepository.save(any())).thenReturn(predecessor);

        DnaRule result = ruleService.supersede("rule-new", "rule-old", "actor");

        assertEquals("superseded", predecessor.getStatus());
        assertEquals("rule-old", successor.getSupersedesId());
    }

    @Test
    void supersede_throwsOnDomainMismatch() {
        DnaRule successor = new DnaRule();
        successor.setId("rule-new");
        successor.setDomainId("domain-1");
        DnaRule predecessor = new DnaRule();
        predecessor.setId("rule-old");
        predecessor.setDomainId("domain-2");
        when(ruleRepository.findById("rule-new")).thenReturn(Optional.of(successor));
        when(ruleRepository.findById("rule-old")).thenReturn(Optional.of(predecessor));

        assertThrows(IllegalArgumentException.class, () -> {
            ruleService.supersede("rule-new", "rule-old", "actor");
        });
    }

    @Test
    void supersede_throwsWhenPredecessorNotFound() {
        DnaRule successor = new DnaRule();
        successor.setId("rule-new");
        successor.setDomainId("domain-1");
        when(ruleRepository.findById("rule-new")).thenReturn(Optional.of(successor));
        when(ruleRepository.findById("rule-old")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            ruleService.supersede("rule-new", "rule-old", "actor");
        });
    }

    @Test
    void findById_returnsPresent() {
        DnaRule rule = new DnaRule();
        rule.setId("rule-1");
        when(ruleRepository.findById("rule-1")).thenReturn(Optional.of(rule));

        Optional<DnaRule> result = ruleService.findById("rule-1");

        assertTrue(result.isPresent());
        assertEquals("rule-1", result.get().getId());
    }

    @Test
    void findByDomain_delegates() {
        DnaRule rule = new DnaRule();
        rule.setId("rule-1");
        when(ruleRepository.findByDomainId("domain-1")).thenReturn(List.of(rule));

        List<DnaRule> result = ruleService.findByDomain("domain-1");

        assertEquals(1, result.size());
    }
}
