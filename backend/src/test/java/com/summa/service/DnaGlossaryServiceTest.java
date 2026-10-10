package com.summa.service;

import com.summa.repository.DnaGlossaryRepository;
import com.summa.repository.DnaDomainRepository;
import com.summa.model.DnaGlossary;
import com.summa.model.DnaDomain;
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
class DnaGlossaryServiceTest {

    @Mock
    private DnaGlossaryRepository glossaryRepository;

    @Mock
    private DnaDomainRepository domainRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SecretsScanner secretsScanner;

    @InjectMocks
    private DnaGlossaryService glossaryService;

    @Test
    void create_setsDefaults() {
        when(glossaryRepository.findByTermAndDomainId("term", "domain-1")).thenReturn(Optional.empty());
        when(glossaryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(domainRepository.findById("domain-1")).thenReturn(Optional.of(new DnaDomain()));

        DnaGlossary result = glossaryService.create("gloss-1", "domain-1", "term", "Definition", "alias1,alias2", "admin");

        assertNotNull(result);
        assertEquals("gloss-1", result.getId());
        assertEquals("term", result.getTerm());
        assertEquals("active", result.getStatus());
        assertEquals("alias1,alias2", result.getAliases());
    }

    @Test
    void create_rejectsDuplicateActiveTerm() {
        DnaGlossary existing = new DnaGlossary();
        existing.setTerm("term");
        existing.setStatus("active");
        when(domainRepository.findById("domain-1")).thenReturn(Optional.of(new DnaDomain()));
        when(glossaryRepository.findByTermAndDomainId("term", "domain-1")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () ->
            glossaryService.create("gloss-2", "domain-1", "term", "Def", null, "admin")
        );
    }

    @Test
    void create_allowsDuplicateRetiredTerm() {
        DnaGlossary existing = new DnaGlossary();
        existing.setTerm("term");
        existing.setStatus("retired");
        when(domainRepository.findById("domain-1")).thenReturn(Optional.of(new DnaDomain()));
        when(glossaryRepository.findByTermAndDomainId("term", "domain-1")).thenReturn(Optional.of(existing));
        when(glossaryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaGlossary result = glossaryService.create("gloss-2", "domain-1", "term", "New Def", null, "admin");

        assertNotNull(result);
        assertEquals("gloss-2", result.getId());
    }

    @Test
    void create_passesSecretScan() {
        when(glossaryRepository.findByTermAndDomainId("term", "domain-1")).thenReturn(Optional.empty());
        when(glossaryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(domainRepository.findById("domain-1")).thenReturn(Optional.of(new DnaDomain()));
        doReturn(false).when(secretsScanner).hasSecrets(anyString());

        glossaryService.create("gloss-1", "domain-1", "term", "def", null, "admin");

        verify(secretsScanner, times(2)).hasSecrets(anyString());
    }

    @Test
    void findById_returnsPresent() {
        DnaGlossary entry = new DnaGlossary();
        entry.setId("gloss-1");
        when(glossaryRepository.findById("gloss-1")).thenReturn(Optional.of(entry));

        Optional<DnaGlossary> result = glossaryService.findById("gloss-1");

        assertTrue(result.isPresent());
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(glossaryRepository.findById("gloss-1")).thenReturn(Optional.empty());

        Optional<DnaGlossary> result = glossaryService.findById("gloss-1");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByDomain_returnsEntries() {
        DnaGlossary entry = new DnaGlossary();
        entry.setId("gloss-1");
        when(glossaryRepository.findByDomainId("domain-1")).thenReturn(List.of(entry));

        List<DnaGlossary> results = glossaryService.findByDomain("domain-1");

        assertEquals(1, results.size());
    }

    @Test
    void findAllActive_filtersToActiveOnly() {
        DnaGlossary active = new DnaGlossary();
        active.setId("gloss-1");
        active.setStatus("active");
        DnaGlossary retired = new DnaGlossary();
        retired.setId("gloss-2");
        retired.setStatus("retired");
        when(glossaryRepository.findAll()).thenReturn(List.of(active, retired));

        List<DnaGlossary> results = glossaryService.findAllActive();

        assertEquals(1, results.size());
        assertEquals("gloss-1", results.get(0).getId());
    }

    @Test
    void retire_changesStatus() {
        DnaGlossary entry = new DnaGlossary();
        entry.setId("gloss-1");
        entry.setStatus("active");
        when(glossaryRepository.findById("gloss-1")).thenReturn(Optional.of(entry));
        when(glossaryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaGlossary result = glossaryService.retire("gloss-1", "admin");

        assertEquals("retired", result.getStatus());
    }

    @Test
    void retire_throwsWhenNotFound() {
        when(glossaryRepository.findById("gloss-1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            glossaryService.retire("gloss-1", "admin")
        );
    }

    @Test
    void update_allowsActiveEntry() {
        DnaGlossary entry = new DnaGlossary();
        entry.setId("gloss-1");
        entry.setStatus("active");
        entry.setDefinition("old def");
        when(glossaryRepository.findById("gloss-1")).thenReturn(Optional.of(entry));
        when(glossaryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaGlossary result = glossaryService.update("gloss-1", "new def", null, "admin");

        assertEquals("new def", result.getDefinition());
    }

    @Test
    void update_allowsDraftEntry() {
        DnaGlossary entry = new DnaGlossary();
        entry.setId("gloss-1");
        entry.setStatus("draft");
        when(glossaryRepository.findById("gloss-1")).thenReturn(Optional.of(entry));
        when(glossaryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaGlossary result = glossaryService.update("gloss-1", "new def", "alias", "admin");

        assertEquals("new def", result.getDefinition());
        assertEquals("alias", result.getAliases());
    }

    @Test
    void update_rejectsRetiredEntry() {
        DnaGlossary entry = new DnaGlossary();
        entry.setId("gloss-1");
        entry.setStatus("retired");
        when(glossaryRepository.findById("gloss-1")).thenReturn(Optional.of(entry));

        assertThrows(IllegalArgumentException.class, () ->
            glossaryService.update("gloss-1", "new def", null, "admin")
        );
    }

    @Test
    void update_throwsWhenNotFound() {
        when(glossaryRepository.findById("gloss-1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            glossaryService.update("gloss-1", "new def", null, "admin")
        );
    }
}
