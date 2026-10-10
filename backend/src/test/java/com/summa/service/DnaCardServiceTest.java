package com.summa.service;

import com.summa.repository.DnaCardRepository;
import com.summa.model.DnaCard;
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
class DnaCardServiceTest {

    @Mock
    private DnaCardRepository cardRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SecretsScanner secretsScanner;

    @InjectMocks
    private DnaCardService cardService;

    @Test
    void create_setsDefaults() {
        when(cardRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doReturn(List.of()).when(secretsScanner).scan(anyString());

        DnaCard result = cardService.create("card-1", "domain-1", "Title", "Definition", null, "admin");

        assertNotNull(result);
        assertEquals("card-1", result.getId());
        assertEquals("domain-1", result.getDomainId());
        assertEquals("active", result.getStatus());
        assertEquals("{}", result.getProvenance());
    }

    @Test
    void create_usesProvidedProvenance() {
        when(cardRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doReturn(List.of()).when(secretsScanner).scan(anyString());

        DnaCard result = cardService.create("card-1", "domain-1", "Title", "Definition", "my-prov", "admin");

        assertEquals("my-prov", result.getProvenance());
    }

    @Test
    void findById_returnsPresent() {
        DnaCard card = new DnaCard();
        card.setId("card-1");
        when(cardRepository.findById("card-1")).thenReturn(Optional.of(card));

        Optional<DnaCard> result = cardService.findById("card-1");

        assertTrue(result.isPresent());
        assertEquals("card-1", result.get().getId());
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(cardRepository.findById("card-1")).thenReturn(Optional.empty());

        Optional<DnaCard> result = cardService.findById("card-1");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByDomain_returnsActiveCards() {
        DnaCard card = new DnaCard();
        card.setId("card-1");
        card.setStatus("active");
        when(cardRepository.findActiveByDomain("domain-1")).thenReturn(List.of(card));

        List<DnaCard> results = cardService.findByDomain("domain-1");

        assertEquals(1, results.size());
        assertEquals("card-1", results.get(0).getId());
    }

    @Test
    void findAllActive_filtersToActiveOnly() {
        DnaCard active = new DnaCard();
        active.setId("card-1");
        active.setStatus("active");
        DnaCard retired = new DnaCard();
        retired.setId("card-2");
        retired.setStatus("retired");
        when(cardRepository.findAll()).thenReturn(List.of(active, retired));

        List<DnaCard> results = cardService.findAllActive();

        assertEquals(1, results.size());
        assertEquals("card-1", results.get(0).getId());
    }

    @Test
    void update_allowsActiveCard() {
        DnaCard card = new DnaCard();
        card.setId("card-1");
        card.setStatus("active");
        card.setTitle("Old Title");
        when(cardRepository.findById("card-1")).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaCard result = cardService.update("card-1", "New Title", null, null, "admin");

        assertEquals("New Title", result.getTitle());
        assertEquals("active", result.getStatus());
    }

    @Test
    void update_allowsDraftCard() {
        DnaCard card = new DnaCard();
        card.setId("card-1");
        card.setStatus("draft");
        when(cardRepository.findById("card-1")).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaCard result = cardService.update("card-1", "New Title", "new def", null, "admin");

        assertEquals("New Title", result.getTitle());
        assertEquals("new def", result.getDefinitionMd());
    }

    @Test
    void update_rejectsRetiredCard() {
        DnaCard card = new DnaCard();
        card.setId("card-1");
        card.setStatus("retired");
        when(cardRepository.findById("card-1")).thenReturn(Optional.of(card));

        assertThrows(IllegalArgumentException.class, () ->
            cardService.update("card-1", "New Title", null, null, "admin")
        );
    }

    @Test
    void update_throwsWhenNotFound() {
        when(cardRepository.findById("card-1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            cardService.update("card-1", "New Title", null, null, "admin")
        );
    }

    @Test
    void retire_changesStatus() {
        DnaCard card = new DnaCard();
        card.setId("card-1");
        card.setStatus("active");
        when(cardRepository.findById("card-1")).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DnaCard result = cardService.retire("card-1", "admin");

        assertEquals("retired", result.getStatus());
    }

    @Test
    void retire_throwsWhenNotFound() {
        when(cardRepository.findById("card-1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            cardService.retire("card-1", "admin")
        );
    }

    @Test
    void createDraft_setsDraftStatus() {
        when(cardRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().doReturn(List.of()).when(secretsScanner).scan(anyString());

        DnaCard result = cardService.createDraft("card-draft", "domain-1", "Draft Title", "Def", "prov", "admin");

        assertEquals("draft", result.getStatus());
        assertEquals("card-draft", result.getId());
    }
}
