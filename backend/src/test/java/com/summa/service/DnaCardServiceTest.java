package com.summa.service;

import com.summa.repository.DnaCardRepository;
import com.summa.model.DnaCard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.summa.exception.EntityNotFoundException;

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
    void create_cardWithDefaults() {
        DnaCard card = new DnaCard();
        card.setId("card-1");
        card.setTitle("Test Card");
        card.setStatus("active");
        when(cardRepository.save(any())).thenReturn(card);

        DnaCard result = cardService.create("card-1", "domain-1", "Test Card", "Definition", "{}", "actor");

        assertNotNull(result);
        assertEquals("active", result.getStatus());
    }

    @Test
    void retire_cardSetsRetired() {
        DnaCard card = new DnaCard();
        card.setId("card-1");
        card.setStatus("active");
        when(cardRepository.findById("card-1")).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenReturn(card);

        DnaCard result = cardService.retire("card-1", "actor");

        assertEquals("retired", result.getStatus());
    }

    @Test
    void retire_throwsWhenNotFound() {
        when(cardRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            cardService.retire("nonexistent", "actor");
        });
    }

    @Test
    void update_allowsActiveCard() {
        DnaCard card = new DnaCard();
        card.setId("card-1");
        card.setStatus("active");
        card.setTitle("Old Title");
        when(cardRepository.findById("card-1")).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DnaCard result = cardService.update("card-1", "New Title", null, null, "actor");

        assertEquals("New Title", result.getTitle());
    }

    @Test
    void update_allowsDraftCard() {
        DnaCard card = new DnaCard();
        card.setId("card-1");
        card.setStatus("draft");
        when(cardRepository.findById("card-1")).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenReturn(card);

        DnaCard result = cardService.update("card-1", "New Title", null, null, "actor");

        assertEquals("New Title", result.getTitle());
    }

    @Test
    void update_throwsWhenRetired() {
        DnaCard card = new DnaCard();
        card.setId("card-1");
        card.setStatus("retired");
        when(cardRepository.findById("card-1")).thenReturn(Optional.of(card));

        assertThrows(IllegalArgumentException.class, () -> {
            cardService.update("card-1", "New Title", null, null, "actor");
        });
    }

    @Test
    void createDraft_createsDraftStatus() {
        DnaCard card = new DnaCard();
        card.setId("card-draft-1");
        card.setTitle("Draft Card");
        card.setStatus("draft");
        card.setDomainId("domain-1");
        when(cardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DnaCard result = cardService.createDraft("card-draft-1", "domain-1", "Draft Card",
            "Definition", "{}", "actor");

        assertEquals("draft", result.getStatus());
        assertEquals("domain-1", result.getDomainId());
    }

    @Test
    void findById_returnsPresent() {
        DnaCard card = new DnaCard();
        card.setId("card-1");
        when(cardRepository.findById("card-1")).thenReturn(Optional.of(card));

        Optional<DnaCard> result = cardService.findById("card-1");

        assertTrue(result.isPresent());
    }

    @Test
    void findByDomain_delegates() {
        DnaCard card = new DnaCard();
        card.setId("card-1");
        when(cardRepository.findActiveByDomain("domain-1")).thenReturn(List.of(card));

        var result = cardService.findByDomain("domain-1");

        assertEquals(1, result.size());
    }

    @Test
    void findAllActive_filtersInactive() {
        DnaCard active = new DnaCard();
        active.setId("card-1");
        active.setStatus("active");
        DnaCard retired = new DnaCard();
        retired.setId("card-2");
        retired.setStatus("retired");
        when(cardRepository.findAll()).thenReturn(List.of(active, retired));

        var result = cardService.findAllActive();

        assertEquals(1, result.size());
        assertEquals("card-1", result.get(0).getId());
    }
}
