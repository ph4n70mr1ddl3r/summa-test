package com.summa.service;

import com.summa.repository.DnaCardRepository;
import com.summa.model.DnaCard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

        assertThrows(IllegalArgumentException.class, () -> {
            cardService.retire("nonexistent", "actor");
        });
    }
}
