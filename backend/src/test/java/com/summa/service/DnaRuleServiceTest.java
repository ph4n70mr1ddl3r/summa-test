package com.summa.service;

import com.summa.repository.DnaRuleRepository;
import com.summa.model.DnaRule;
import com.summa.exception.EntityNotFoundException;
import com.summa.model.DnaDomain;
import org.junit.jupiter.api.BeforeEach;
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

    /**
     * DGV-041: a rule write names its domain, so every rule door resolves the
     * domain first. The default fixture is an active domain — the archived
     * compartment has its own test.
     */
    @BeforeEach
    void stubActiveDomain() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setStatus("active");
        lenient().when(domainService.findById(anyString())).thenReturn(Optional.of(domain));
    }

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
    void create_rejectsMissingSupersedes() {
        when(ruleRepository.findById("rule-future")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            ruleService.create("rule-1", "domain-1", "stmt", null, null, null, "rule-future", "admin");
        });
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
        // DNC-023: the displacement edge is the superseder's effective_from.
        successor.setEffectiveFrom(Instant.now().minusSeconds(60));
        when(ruleRepository.findById("rule-new")).thenReturn(Optional.of(successor));
        when(ruleRepository.findById("rule-old")).thenReturn(Optional.of(predecessor));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaRule result = ruleService.supersede("rule-new", "rule-old", "admin");

        assertEquals("rule-old", successor.getSupersedesId());
        assertEquals("superseded", predecessor.getStatus());
        assertEquals("rule-old", result.getId());
    }

    @Test
    void supersede_keepsThePredecessorLiveUntilTheSuccessorsWindowOpens() {
        // DNC-023: "The displacement edge is the superseder's `effective_from`: a
        // predecessor keeps injecting until its superseder's window opens; a
        // future-windowed successor is a scheduled replacement, never a normative
        // gap." The predecessor's window is truncated at the edge, so it stops
        // injecting exactly when the successor starts — no gap before the edge and
        // no co-temporal pair after it.
        DnaRule predecessor = new DnaRule();
        predecessor.setId("rule-old");
        predecessor.setDomainId("domain-1");
        predecessor.setStatus("active");
        DnaRule successor = new DnaRule();
        successor.setId("rule-new");
        successor.setDomainId("domain-1");
        successor.setStatus("active");
        Instant edge = Instant.now().plusSeconds(3600);
        successor.setEffectiveFrom(edge);
        when(ruleRepository.findById("rule-new")).thenReturn(Optional.of(successor));
        when(ruleRepository.findById("rule-old")).thenReturn(Optional.of(predecessor));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ruleService.supersede("rule-new", "rule-old", "admin");

        assertEquals(edge, predecessor.getEffectiveTo());
        // The predecessor still injects before the edge — it is not displaced yet.
        assertEquals("active", predecessor.getStatus());
    }

    @Test
    void supersede_neverShortensAnAlreadyEarlierWindow() {
        // The displacement edge only ever shortens the predecessor's window.
        DnaRule predecessor = new DnaRule();
        predecessor.setId("rule-old");
        predecessor.setDomainId("domain-1");
        predecessor.setStatus("active");
        Instant ownEnd = Instant.now().plusSeconds(60);
        predecessor.setEffectiveTo(ownEnd);
        DnaRule successor = new DnaRule();
        successor.setId("rule-new");
        successor.setDomainId("domain-1");
        successor.setStatus("active");
        successor.setEffectiveFrom(Instant.now().plusSeconds(7200));
        when(ruleRepository.findById("rule-new")).thenReturn(Optional.of(successor));
        when(ruleRepository.findById("rule-old")).thenReturn(Optional.of(predecessor));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ruleService.supersede("rule-new", "rule-old", "admin");

        assertEquals(ownEnd, predecessor.getEffectiveTo());
    }

    @Test
    void create_displacesThePredecessorAtTheSuccessorsWindow() {
        // DNC-010/DNC-023: naming a predecessor on the create door is a
        // displacement too — otherwise the chain kept two co-temporal heads.
        DnaRule predecessor = new DnaRule();
        predecessor.setId("rule-old");
        predecessor.setDomainId("domain-1");
        predecessor.setStatus("active");
        when(ruleRepository.findById("rule-old")).thenReturn(Optional.of(predecessor));
        when(ruleRepository.findBySupersedesId("rule-old")).thenReturn(List.of());
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doReturn(List.of()).when(secretsScanner).scan(anyString());

        Instant edge = Instant.now().minusSeconds(60);
        ruleService.create("rule-new", "domain-1", "stmt", null, edge, null, "rule-old", "admin");

        assertEquals(edge, predecessor.getEffectiveTo());
        assertEquals("superseded", predecessor.getStatus());
    }

    @Test
    void create_refusesAnArchivedDomain() {
        // DGV-041: the archived domain row is read-only history — no new items.
        DnaDomain archived = new DnaDomain();
        archived.setId("domain-1");
        archived.setStatus("archived");
        when(domainService.findById("domain-1")).thenReturn(Optional.of(archived));
        lenient().doReturn(List.of()).when(secretsScanner).scan(anyString());

        assertThrows(IllegalStateException.class, () ->
            ruleService.create("rule-1", "domain-1", "stmt", null, null, null, null, "admin")
        );
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void update_runsTheSecretsScan() {
        // DWP-060/SEC-030: an item-level write gets the same secrets scan a
        // reviewed proposal's publish gets — the update door is not a side door.
        DnaRule rule = new DnaRule();
        rule.setId("rule-1");
        rule.setStatus("active");
        rule.setDomainId("domain-1");
        when(ruleRepository.findById("rule-1")).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doReturn(List.of()).when(secretsScanner).scan(anyString());

        ruleService.update("rule-1", "New statement", null, null, "admin");

        verify(secretsScanner).hasSecrets("New statement");
    }

    @Test
    void update_rejectsAnEffectiveToBeforeEffectiveFrom() {
        // DWP-062/DWP-063: one validation at every door — window ordering is
        // checked on update exactly as it is on create.
        DnaRule rule = new DnaRule();
        rule.setId("rule-1");
        rule.setStatus("active");
        rule.setDomainId("domain-1");
        rule.setEffectiveFrom(Instant.now());
        when(ruleRepository.findById("rule-1")).thenReturn(Optional.of(rule));

        Instant before = Instant.now().minusSeconds(3600);
        assertThrows(IllegalArgumentException.class, () ->
            ruleService.update("rule-1", null, null, before, "admin")
        );
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void retire_truncatesTheWindow() {
        // DNC-021/DWP-061: a rule's retire is window truncation — effective_to
        // pinned to now, the row lapsing at that boundary. The item surface is
        // create / update / retire, never delete.
        DnaRule rule = new DnaRule();
        rule.setId("rule-1");
        rule.setStatus("active");
        rule.setDomainId("domain-1");
        when(ruleRepository.findById("rule-1")).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaRule result = ruleService.retire("rule-1", "admin");

        assertEquals("lapsed", result.getStatus());
        assertNotNull(result.getEffectiveTo());
    }

    @Test
    void lapseExpiredRules_marksAnExpiredRuleLapsed() {
        // DNC-026: ordinary expiring rules transition `lapsed` at effective_to.
        DnaRule rule = new DnaRule();
        rule.setId("rule-1");
        rule.setStatus("active");
        rule.setDomainId("domain-1");
        rule.setEffectiveTo(Instant.now().minusSeconds(60));
        when(ruleRepository.findByStatus("active")).thenReturn(List.of(rule));
        when(ruleRepository.findBySupersedesId("rule-1")).thenReturn(List.of());
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ruleService.lapseExpiredRules();

        assertEquals("lapsed", rule.getStatus());
    }

    @Test
    void lapseExpiredRules_keepsADisplacedPredecessorSuperseded() {
        // DNC-022: a predecessor stays superseded when its superseder lapses —
        // nothing flips back silently.
        DnaRule rule = new DnaRule();
        rule.setId("rule-1");
        rule.setStatus("active");
        rule.setDomainId("domain-1");
        rule.setEffectiveTo(Instant.now().minusSeconds(60));
        DnaRule successor = new DnaRule();
        successor.setId("rule-2");
        successor.setEffectiveFrom(Instant.now().minusSeconds(120));
        when(ruleRepository.findByStatus("active")).thenReturn(List.of(rule));
        when(ruleRepository.findBySupersedesId("rule-1")).thenReturn(List.of(successor));
        when(ruleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ruleService.lapseExpiredRules();

        assertEquals("superseded", rule.getStatus());
    }

    @Test
    void supersede_refusesAForkedChain() {
        // DNC-025: supersession chains are linear, not forks — a second live
        // `supersedes_id` edge onto an already-superseded row is refused at
        // propose, amend, and item write; displacing a superseded rule means
        // naming the chain's live head. The item-write door carried no such
        // guard, so a fork was reachable from the API.
        DnaRule predecessor = new DnaRule();
        predecessor.setId("rule-old");
        predecessor.setDomainId("domain-1");
        predecessor.setStatus("superseded");
        DnaRule successor = new DnaRule();
        successor.setId("rule-new");
        successor.setDomainId("domain-1");
        successor.setStatus("active");
        when(ruleRepository.findById("rule-new")).thenReturn(Optional.of(successor));
        when(ruleRepository.findById("rule-old")).thenReturn(Optional.of(predecessor));
        when(ruleRepository.findBySupersedesId("rule-old")).thenReturn(List.of(predecessor));

        assertThrows(IllegalArgumentException.class, () ->
            ruleService.supersede("rule-new", "rule-old", "admin")
        );
    }

    @Test
    void supersede_refusesALapsedPredecessor() {
        // DNC-022: superseded and lapsed rules are frozen history — an item
        // write must not flip a lapsed rule out of it.
        DnaRule predecessor = new DnaRule();
        predecessor.setId("rule-old");
        predecessor.setDomainId("domain-1");
        predecessor.setStatus("lapsed");
        DnaRule successor = new DnaRule();
        successor.setId("rule-new");
        successor.setDomainId("domain-1");
        successor.setStatus("active");
        when(ruleRepository.findById("rule-new")).thenReturn(Optional.of(successor));
        when(ruleRepository.findById("rule-old")).thenReturn(Optional.of(predecessor));
        when(ruleRepository.findBySupersedesId("rule-old")).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () ->
            ruleService.supersede("rule-new", "rule-old", "admin")
        );
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
