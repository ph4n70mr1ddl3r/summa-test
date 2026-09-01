package com.summa.service;

import com.summa.repository.DnaDomainRepository;
import com.summa.repository.DnaCardRepository;
import com.summa.repository.DnaProposalRepository;
import com.summa.repository.WorkspaceRepository;
import com.summa.repository.DnaRuleRepository;
import com.summa.repository.DnaGlossaryRepository;
import com.summa.repository.DnaGoalRepository;
import com.summa.repository.DnaDecisionRepository;
import com.summa.repository.DataHoldRepository;
import com.summa.repository.AskRepository;
import com.summa.model.DnaDomain;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DnaDomainServiceTest {

    @Mock
    private DnaDomainRepository domainRepository;

    @Mock
    private DnaCardRepository cardRepository;

    @Mock
    private DnaProposalRepository proposalRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private DnaRuleRepository ruleRepository;

    @Mock
    private DnaGlossaryRepository glossaryRepository;

    @Mock
    private DnaGoalRepository goalRepository;

    @Mock
    private DnaDecisionRepository decisionRepository;

    @Mock
    private DataHoldRepository dataHoldRepository;

    @Mock
    private AskRepository askRepository;

    @Mock
    private AuditService auditService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DnaDomainService domainService;

    @Test
    void create_domainWithDefaults() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setName("Engineering");
        domain.setAccess("public");
        domain.setStore("git");
        domain.setReviewSlaDays(7);
        when(domainRepository.save(any())).thenReturn(domain);

        DnaDomain result = domainService.create("domain-1", "Engineering", "human-1", null, null, null, null);

        assertNotNull(result);
        assertEquals("Engineering", result.getName());
    }

    @Test
    void archive_setsArchivedStatus() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setStatus("active");
        when(domainRepository.findById("domain-1")).thenReturn(Optional.of(domain));
        when(cardRepository.countByDomainIdAndStatusNot("domain-1", "retired")).thenReturn(0L);
        when(proposalRepository.countByDomainIdAndStatus("domain-1", "open")).thenReturn(0L);
        when(workspaceRepository.countByDomainIdsContaining("domain-1")).thenReturn(0L);
        when(domainRepository.save(any())).thenReturn(domain);

        DnaDomain result = domainService.archive("domain-1", "admin");

        assertEquals("archived", result.getStatus());
    }

    @Test
    void archive_throwsWhenNotFound() {
        when(domainRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            domainService.archive("nonexistent", "admin");
        });
    }

    @Test
    void archive_refusesWhenLiveCardsExist() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setStatus("active");
        when(domainRepository.findById("domain-1")).thenReturn(Optional.of(domain));
        when(cardRepository.countByDomainIdAndStatusNot("domain-1", "retired")).thenReturn(3L);

        assertThrows(IllegalStateException.class, () -> {
            domainService.archive("domain-1", "admin");
        });
    }

    @Test
    void split_archivesParentAndCreatesChild() {
        DnaDomain parent = new DnaDomain();
        parent.setId("parent");
        parent.setName("Engineering");
        parent.setAccess("public");
        parent.setStore("git");
        parent.setReviewSlaDays(7);
        parent.setNamedReaders("[]");
        parent.setSod("off");
        parent.setOwnerHumanId("human-1");

        when(domainRepository.findById("parent")).thenReturn(Optional.of(parent));
        when(cardRepository.countByDomainIdAndStatusNot("parent", "retired")).thenReturn(1L);
        when(proposalRepository.countByDomainIdAndStatus("parent", "open")).thenReturn(0L);
        when(workspaceRepository.countByDomainIdsContaining("parent")).thenReturn(1L);
        when(dataHoldRepository.existsByKindAndSubjectIdAndReleasedAtIsNull("domain", "parent")).thenReturn(false);
        when(domainRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<DnaDomain> children = domainService.split("parent", "admin",
            "human-2", "domain", null, null, null, null,
            List.of("card-1"), List.of("ws-1"), List.of());

        assertEquals(1, children.size());
        assertEquals("active", children.get(0).getStatus());
        assertEquals("human-2", children.get(0).getOwnerHumanId());
        verify(domainRepository).save(argThat(d -> "archived".equals(d.getStatus())));
    }

    @Test
    void split_refusesUnderHold() {
        DnaDomain parent = new DnaDomain();
        parent.setId("parent");
        when(domainRepository.findById("parent")).thenReturn(Optional.of(parent));
        when(dataHoldRepository.existsByKindAndSubjectIdAndReleasedAtIsNull("domain", "parent")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> {
            domainService.split("parent", "admin", null, null, null, null, null, null,
                List.of(), List.of(), List.of());
        });
    }

    @Test
    void split_refusesWhenMappingNotTotal() {
        DnaDomain parent = new DnaDomain();
        parent.setId("parent");
        when(domainRepository.findById("parent")).thenReturn(Optional.of(parent));
        when(dataHoldRepository.existsByKindAndSubjectIdAndReleasedAtIsNull("domain", "parent")).thenReturn(false);
        when(cardRepository.countByDomainIdAndStatusNot("parent", "retired")).thenReturn(3L);
        when(proposalRepository.countByDomainIdAndStatus("parent", "open")).thenReturn(0L);
        when(workspaceRepository.countByDomainIdsContaining("parent")).thenReturn(0L);

        assertThrows(IllegalStateException.class, () -> {
            domainService.split("parent", "admin", null, null, null, null, null, null,
                List.of("card-1"), List.of(), List.of());
        });
    }

    @Test
    void merge_appliesNarrowerAccess() {
        DnaDomain source = new DnaDomain();
        source.setId("src");
        source.setAccess("named");
        source.setNamedReaders("[\"a\"]");
        source.setStatus("active");
        source.setOwnerHumanId("human-src");

        DnaDomain survivor = new DnaDomain();
        survivor.setId("surv");
        survivor.setAccess("public");
        survivor.setNamedReaders("[]");
        survivor.setStatus("active");
        survivor.setOwnerHumanId("human-surv");

        when(domainRepository.findById("src")).thenReturn(Optional.of(source));
        when(domainRepository.findById("surv")).thenReturn(Optional.of(survivor));
        when(dataHoldRepository.existsByKindAndSubjectIdAndReleasedAtIsNull(eq("domain"), any())).thenReturn(false);
        when(cardRepository.findByDomainId("src")).thenReturn(List.of());
        when(ruleRepository.findByDomainId("src")).thenReturn(List.of());
        when(decisionRepository.findByDomainId("src")).thenReturn(List.of());
        when(glossaryRepository.findByDomainId("src")).thenReturn(List.of());
        when(goalRepository.findByDomainId("src")).thenReturn(List.of());
        when(proposalRepository.findByDomainId("src")).thenReturn(List.of());
        when(workspaceRepository.findAll()).thenReturn(List.of());
        when(domainRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(askRepository.findByToAndStatusPending(any())).thenReturn(List.of());

        DnaDomain result = domainService.merge("src", "surv", "admin", null, null);

        assertEquals("named", result.getAccess());
        assertEquals("archived", source.getStatus());
    }

    @Test
    void merge_refusesWhenAccessCannotBeComputed() {
        DnaDomain source = new DnaDomain();
        source.setId("src");
        source.setAccess("named");
        source.setNamedReaders("[\"a\"]");
        source.setStatus("active");

        DnaDomain survivor = new DnaDomain();
        survivor.setId("surv");
        survivor.setAccess("named");
        survivor.setNamedReaders("[\"b\"]");
        survivor.setStatus("active");

        when(domainRepository.findById("src")).thenReturn(Optional.of(source));
        when(domainRepository.findById("surv")).thenReturn(Optional.of(survivor));
        lenient().when(dataHoldRepository.existsByKindAndSubjectIdAndReleasedAtIsNull(eq("domain"), any())).thenReturn(false);
        lenient().when(cardRepository.findByDomainId("src")).thenReturn(List.of());
        lenient().when(ruleRepository.findByDomainId("src")).thenReturn(List.of());
        lenient().when(decisionRepository.findByDomainId("src")).thenReturn(List.of());
        lenient().when(glossaryRepository.findByDomainId("src")).thenReturn(List.of());
        lenient().when(goalRepository.findByDomainId("src")).thenReturn(List.of());
        lenient().when(proposalRepository.findByDomainId("src")).thenReturn(List.of());
        lenient().when(workspaceRepository.findAll()).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> {
            domainService.merge("src", "surv", "admin", null, null);
        });
    }
}
