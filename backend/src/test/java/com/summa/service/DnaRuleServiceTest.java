package com.summa.service;

import com.summa.repository.DnaRuleRepository;
import com.summa.model.DnaRule;
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

        assertThrows(IllegalStateException.class, () -> {
            ruleService.update("rule-1", null, null, null, "actor");
        });
    }
}
