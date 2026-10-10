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
import com.summa.repository.InitiativeRepository;
import com.summa.model.DnaDomain;
import com.summa.model.DnaCard;
import com.summa.model.DnaProposal;
import com.summa.model.Workspace;
import com.summa.exception.EntityNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DnaDomainServiceTest {

    @Mock private DnaDomainRepository domainRepository;
    @Mock private DnaCardRepository cardRepository;
    @Mock private DnaProposalRepository proposalRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private DnaRuleRepository ruleRepository;
    @Mock private DnaGlossaryRepository glossaryRepository;
    @Mock private DnaGoalRepository goalRepository;
    @Mock private DnaDecisionRepository decisionRepository;
    @Mock private DataHoldRepository dataHoldRepository;
    @Mock private AskRepository askRepository;
    @Mock private InitiativeRepository initiativeRepository;
    @Mock private AuditService auditService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DnaDomainService domainService;

    @Test
    void create_setsDefaults() {
        when(domainRepository.save(any())).thenAnswer(invocation -> {
            DnaDomain d = invocation.getArgument(0);
            if (d.getId() == null) d.setId("domain-1");
            return d;
        });

        DnaDomain result = domainService.create("domain-1", "My Domain", "h:admin", null, null, null, null, "admin");

        assertNotNull(result);
        assertEquals("domain-1", result.getId());
        assertEquals("My Domain", result.getName());
        assertEquals("public", result.getAccess());
        assertEquals("git", result.getStore());
        assertEquals(Integer.valueOf(7), result.getReviewSlaDays());
    }

    @Test
    void findById_returnsPresent() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        when(domainRepository.findById("domain-1")).thenReturn(Optional.of(domain));

        Optional<DnaDomain> result = domainService.findById("domain-1");

        assertTrue(result.isPresent());
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(domainRepository.findById("domain-1")).thenReturn(Optional.empty());

        Optional<DnaDomain> result = domainService.findById("domain-1");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByName_returnsActiveOnly() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setName("My Domain");
        when(domainRepository.findByNameNotArchived("My Domain")).thenReturn(Optional.of(domain));

        Optional<DnaDomain> result = domainService.findByName("My Domain");

        assertTrue(result.isPresent());
    }

    @Test
    void findAll_returnsActiveDomains() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        when(domainRepository.findAllActive()).thenReturn(List.of(domain));

        List<DnaDomain> results = domainService.findAll();

        assertEquals(1, results.size());
    }

    @Test
    void findAllIncludingArchived_returnsAll() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        when(domainRepository.findAll()).thenReturn(List.of(domain));

        List<DnaDomain> results = domainService.findAllIncludingArchived();

        assertEquals(1, results.size());
    }

    @Test
    void archive_succeedsWhenNoLiveState() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setStatus("active");
        when(domainRepository.findById("domain-1")).thenReturn(Optional.of(domain));
        when(cardRepository.countByDomainIdAndStatusNot("domain-1", "retired")).thenReturn(0L);
        when(proposalRepository.countByDomainIdAndStatus("domain-1", "open")).thenReturn(0L);
        when(workspaceRepository.countByDomainIdsContaining("domain-1")).thenReturn(0L);
        when(domainRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaDomain result = domainService.archive("domain-1", "admin");

        assertEquals("archived", result.getStatus());
    }

    @Test
    void archive_refusesWhenLiveCardsExist() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setStatus("active");
        when(domainRepository.findById("domain-1")).thenReturn(Optional.of(domain));
        when(cardRepository.countByDomainIdAndStatusNot("domain-1", "retired")).thenReturn(1L);

        assertThrows(IllegalStateException.class, () ->
            domainService.archive("domain-1", "admin")
        );
    }

    @Test
    void archive_refusesWhenOpenProposalsExist() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setStatus("active");
        when(domainRepository.findById("domain-1")).thenReturn(Optional.of(domain));
        when(cardRepository.countByDomainIdAndStatusNot("domain-1", "retired")).thenReturn(0L);
        when(proposalRepository.countByDomainIdAndStatus("domain-1", "open")).thenReturn(1L);

        assertThrows(IllegalStateException.class, () ->
            domainService.archive("domain-1", "admin")
        );
    }

    @Test
    void archive_refusesWhenLiveBindingsExist() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setStatus("active");
        when(domainRepository.findById("domain-1")).thenReturn(Optional.of(domain));
        when(cardRepository.countByDomainIdAndStatusNot("domain-1", "retired")).thenReturn(0L);
        when(proposalRepository.countByDomainIdAndStatus("domain-1", "open")).thenReturn(0L);
        when(workspaceRepository.countByDomainIdsContaining("domain-1")).thenReturn(1L);

        assertThrows(IllegalStateException.class, () ->
            domainService.archive("domain-1", "admin")
        );
    }

    @Test
    void archive_throwsWhenNotFound() {
        when(domainRepository.findById("domain-1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            domainService.archive("domain-1", "admin")
        );
    }

    @Test
    void rename_updatesName() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setName("Old Name");
        when(domainRepository.findById("domain-1")).thenReturn(Optional.of(domain));
        when(domainRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaDomain result = domainService.rename("domain-1", "New Name", "admin");

        assertEquals("New Name", result.getName());
    }

    @Test
    void rename_throwsWhenNotFound() {
        when(domainRepository.findById("domain-1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            domainService.rename("domain-1", "New Name", "admin")
        );
    }

    @Test
    void updateOwner_changesOwnerId() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setOwnerHumanId("h:old-owner");
        when(domainRepository.findById("domain-1")).thenReturn(Optional.of(domain));
        when(domainRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaDomain result = domainService.updateOwner("domain-1", "h:new-owner", "admin");

        assertEquals("h:new-owner", result.getOwnerHumanId());
    }

    @Test
    void updateAccess_changesAccess() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setAccess("public");
        when(domainRepository.findById("domain-1")).thenReturn(Optional.of(domain));
        when(domainRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaDomain result = domainService.updateAccess("domain-1", "private", "admin");

        assertEquals("private", result.getAccess());
    }

    @Test
    void split_createsChildDomain() {
        DnaDomain parent = new DnaDomain();
        parent.setId("parent-1");
        parent.setName("Parent");
        parent.setAccess("public");
        parent.setStore("git");
        parent.setReviewSlaDays(7);
        when(domainRepository.findById("parent-1")).thenReturn(Optional.of(parent));
        when(dataHoldRepository.existsByKindAndSubjectIdAndReleasedAtIsNull("domain", "parent-1")).thenReturn(false);
        when(cardRepository.countByDomainIdAndStatusNot("parent-1", "retired")).thenReturn(0L);
        when(proposalRepository.countByDomainIdAndStatus("parent-1", "open")).thenReturn(0L);
        when(workspaceRepository.countByDomainIdsContaining("parent-1")).thenReturn(0L);
        when(domainRepository.save(any())).thenAnswer(invocation -> {
            DnaDomain d = invocation.getArgument(0);
            if (d.getId() == null) d.setId("child-1");
            return d;
        });

        List<DnaDomain> results = domainService.split("parent-1", "admin", "h:owner", "public", "git",
            null, null, null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        assertEquals(1, results.size());
        assertNotNull(results.get(0).getId());
    }

    @Test
    void split_refusesWhenUnderHold() {
        DnaDomain parent = new DnaDomain();
        parent.setId("parent-1");
        parent.setName("Parent");
        when(domainRepository.findById("parent-1")).thenReturn(Optional.of(parent));
        when(dataHoldRepository.existsByKindAndSubjectIdAndReleasedAtIsNull("domain", "parent-1")).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
            domainService.split("parent-1", "admin", null, null, null, null, null, null,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList())
        );
    }

    @Test
    void split_refusesNonTotalMapping() {
        DnaDomain parent = new DnaDomain();
        parent.setId("parent-1");
        parent.setName("Parent");
        when(domainRepository.findById("parent-1")).thenReturn(Optional.of(parent));
        when(dataHoldRepository.existsByKindAndSubjectIdAndReleasedAtIsNull("domain", "parent-1")).thenReturn(false);
        when(cardRepository.countByDomainIdAndStatusNot("parent-1", "retired")).thenReturn(2L);
        when(proposalRepository.countByDomainIdAndStatus("parent-1", "open")).thenReturn(0L);
        when(workspaceRepository.countByDomainIdsContaining("parent-1")).thenReturn(0L);

        assertThrows(IllegalStateException.class, () ->
            domainService.split("parent-1", "admin", null, null, null, null, null, null,
                List.of("card-1"), Collections.emptyList(), Collections.emptyList())
        );
    }

    @Test
    void merge_refusesSelfMerge() {
        assertThrows(IllegalStateException.class, () ->
            domainService.merge("domain-1", "domain-1", "admin", null, null)
        );
    }

    @Test
    void merge_refusesWhenSourceNotFound() {
        when(domainRepository.findById("source-1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            domainService.merge("source-1", "survivor-1", "admin", null, null)
        );
    }

    @Test
    void merge_refusesWhenSurvivorNotFound() {
        DnaDomain source = new DnaDomain();
        source.setId("source-1");
        when(domainRepository.findById("source-1")).thenReturn(Optional.of(source));
        when(domainRepository.findById("survivor-1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            domainService.merge("source-1", "survivor-1", "admin", null, null)
        );
    }

    @Test
    void merge_refusesWhenSourceUnderHold() {
        DnaDomain source = new DnaDomain();
        source.setId("source-1");
        DnaDomain survivor = new DnaDomain();
        survivor.setId("survivor-1");
        when(domainRepository.findById("source-1")).thenReturn(Optional.of(source));
        when(domainRepository.findById("survivor-1")).thenReturn(Optional.of(survivor));
        when(dataHoldRepository.existsByKindAndSubjectIdAndReleasedAtIsNull("domain", "source-1")).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
            domainService.merge("source-1", "survivor-1", "admin", null, null)
        );
    }

    @Test
    void merge_usesDeclaredAccess() {
        DnaDomain source = new DnaDomain();
        source.setId("source-1");
        source.setAccess("public");
        DnaDomain survivor = new DnaDomain();
        survivor.setId("survivor-1");
        survivor.setAccess("private");
        when(domainRepository.findById("source-1")).thenReturn(Optional.of(source));
        when(domainRepository.findById("survivor-1")).thenReturn(Optional.of(survivor));
        lenient().when(dataHoldRepository.existsByKindAndSubjectIdAndReleasedAtIsNull(anyString(), anyString())).thenReturn(false);
        lenient().when(cardRepository.findByDomainId("source-1")).thenReturn(Collections.emptyList());
        lenient().when(ruleRepository.findByDomainId("source-1")).thenReturn(Collections.emptyList());
        lenient().when(decisionRepository.findByDomainId("source-1")).thenReturn(Collections.emptyList());
        lenient().when(glossaryRepository.findByDomainId("source-1")).thenReturn(Collections.emptyList());
        lenient().when(goalRepository.findByDomainId("source-1")).thenReturn(Collections.emptyList());
        lenient().when(proposalRepository.findOpenByDomain("source-1")).thenReturn(Collections.emptyList());
        lenient().when(workspaceRepository.findAll()).thenReturn(Collections.emptyList());
        when(domainRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaDomain result = domainService.merge("source-1", "survivor-1", "admin", "private", null);

        assertEquals("private", result.getAccess());
    }

    @Test
    void merge_computesNarrowerAccess() {
        DnaDomain source = new DnaDomain();
        source.setId("source-1");
        source.setAccess("public");
        DnaDomain survivor = new DnaDomain();
        survivor.setId("survivor-1");
        survivor.setAccess("private");
        when(domainRepository.findById("source-1")).thenReturn(Optional.of(source));
        when(domainRepository.findById("survivor-1")).thenReturn(Optional.of(survivor));
        lenient().when(dataHoldRepository.existsByKindAndSubjectIdAndReleasedAtIsNull(anyString(), anyString())).thenReturn(false);
        lenient().when(cardRepository.findByDomainId("source-1")).thenReturn(Collections.emptyList());
        lenient().when(ruleRepository.findByDomainId("source-1")).thenReturn(Collections.emptyList());
        lenient().when(decisionRepository.findByDomainId("source-1")).thenReturn(Collections.emptyList());
        lenient().when(glossaryRepository.findByDomainId("source-1")).thenReturn(Collections.emptyList());
        lenient().when(goalRepository.findByDomainId("source-1")).thenReturn(Collections.emptyList());
        lenient().when(proposalRepository.findOpenByDomain("source-1")).thenReturn(Collections.emptyList());
        lenient().when(workspaceRepository.findAll()).thenReturn(Collections.emptyList());
        when(domainRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaDomain result = domainService.merge("source-1", "survivor-1", "admin", null, null);

        assertEquals("private", result.getAccess());
    }

    @Test
    void findByOwnerHumanId_returnsDomains() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        when(domainRepository.findByOwnerHumanId("h:admin")).thenReturn(List.of(domain));

        List<DnaDomain> results = domainService.findByOwnerHumanId("h:admin");

        assertEquals(1, results.size());
    }
}
