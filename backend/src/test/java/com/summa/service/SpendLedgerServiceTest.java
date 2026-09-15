package com.summa.service;

import com.summa.repository.SpendLedgerRepository;
import com.summa.model.SpendLedger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.summa.exception.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class SpendLedgerServiceTest {

    @Mock
    private SpendLedgerRepository repository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private SpendLedgerService ledgerService;

    @Test
    void acknowledge_findsAndAcknowledges() {
        SpendLedger ledger = new SpendLedger();
        ledger.setId("sl-1");
        ledger.setAcknowledged(false);
        when(repository.findById("sl-1")).thenReturn(Optional.of(ledger));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        SpendLedger result = ledgerService.acknowledge("sl-1", "admin");

        assertTrue(result.getAcknowledged());
        verify(auditService).log(eq("admin"), eq("ACKNOWLEDGE_OVERRUN"), eq("spend_ledger"), eq("sl-1"), isNull());
    }

    @Test
    void acknowledge_throwsWhenNotFound() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            ledgerService.acknowledge("missing", "admin"));
    }
}
