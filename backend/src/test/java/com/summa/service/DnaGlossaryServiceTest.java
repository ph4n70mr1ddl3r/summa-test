package com.summa.service;

import com.summa.repository.DnaGlossaryRepository;
import com.summa.model.DnaGlossary;
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
class DnaGlossaryServiceTest {

    @Mock
    private DnaGlossaryRepository glossaryRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private DnaGlossaryService glossaryService;

    @Test
    void create_newEntry() {
        DnaGlossary entry = new DnaGlossary();
        entry.setId("g1");
        entry.setStatus("active");
        when(glossaryRepository.findByTermAndDomainId("term1", "d1")).thenReturn(Optional.empty());
        when(glossaryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DnaGlossary result = glossaryService.create("g1", "d1", "term1", "def1", "alias1", "actor");

        assertNotNull(result);
        assertEquals("active", result.getStatus());
        verify(auditService).log(eq("actor"), eq("CREATE_GLOSSARY"), eq("dna_glossary"), eq("g1"), anyString());
    }

    @Test
    void create_throwsWhenDuplicateActiveTerm() {
        DnaGlossary existing = new DnaGlossary();
        existing.setStatus("active");
        when(glossaryRepository.findByTermAndDomainId("term1", "d1")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () ->
            glossaryService.create("g2", "d1", "term1", "def2", null, "actor"));
    }

    @Test
    void create_allowsDuplicateRetiredTerm() {
        DnaGlossary existing = new DnaGlossary();
        existing.setStatus("retired");
        when(glossaryRepository.findByTermAndDomainId("term1", "d1")).thenReturn(Optional.of(existing));

        DnaGlossary entry = new DnaGlossary();
        entry.setId("g2");
        when(glossaryRepository.save(any())).thenReturn(entry);

        assertDoesNotThrow(() -> glossaryService.create("g2", "d1", "term1", "new def", null, "actor"));
    }

    @Test
    void create_defaultsDefinitionAndAliases() {
        DnaGlossary entry = new DnaGlossary();
        entry.setId("g1");
        when(glossaryRepository.findByTermAndDomainId("term1", "d1")).thenReturn(Optional.empty());
        when(glossaryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DnaGlossary result = glossaryService.create("g1", "d1", "term1", null, null, "actor");

        assertEquals("", result.getDefinition());
        assertEquals("[]", result.getAliases());
    }

    @Test
    void retire_findsAndRetires() {
        DnaGlossary entry = new DnaGlossary();
        entry.setId("g1");
        entry.setStatus("active");
        when(glossaryRepository.findById("g1")).thenReturn(Optional.of(entry));
        when(glossaryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DnaGlossary result = glossaryService.retire("g1", "admin");

        assertEquals("retired", result.getStatus());
        verify(auditService).log(eq("admin"), eq("RETIRE_GLOSSARY"), eq("dna_glossary"), eq("g1"), isNull());
    }

    @Test
    void retire_throwsWhenNotFound() {
        when(glossaryRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            glossaryService.retire("missing", "admin"));
    }

    @Test
    void update_updatesActive() {
        DnaGlossary entry = new DnaGlossary();
        entry.setId("g1");
        entry.setStatus("active");
        when(glossaryRepository.findById("g1")).thenReturn(Optional.of(entry));
        when(glossaryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DnaGlossary result = glossaryService.update("g1", "new def", "new alias", "actor");

        assertEquals("new def", result.getDefinition());
        assertEquals("new alias", result.getAliases());
    }

    @Test
    void update_throwsWhenRetired() {
        DnaGlossary entry = new DnaGlossary();
        entry.setId("g1");
        entry.setStatus("retired");
        when(glossaryRepository.findById("g1")).thenReturn(Optional.of(entry));

        assertThrows(IllegalStateException.class, () ->
            glossaryService.update("g1", "new def", null, "actor"));
    }

    @Test
    void update_throwsWhenNotFound() {
        when(glossaryRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            glossaryService.update("missing", "def", null, "actor"));
    }
}
