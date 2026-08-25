package com.summa.service;

import com.summa.repository.SpendLedgerRepository;
import com.summa.model.SpendLedger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;

@Service
public class SpendLedgerService {
    private final SpendLedgerRepository repository;
    private final AuditService auditService;

    public SpendLedgerService(SpendLedgerRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    public Optional<SpendLedger> findById(String id) {
        return repository.findById(id);
    }

    @Transactional
    public SpendLedger acknowledge(String id, String actor) {
        SpendLedger ledger = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Spend ledger row not found: " + id));
        ledger.setAcknowledged(true);
        SpendLedger saved = repository.save(ledger);
        auditService.log(actor, "ACKNOWLEDGE_OVERRUN", "spend_ledger", id, null);
        return saved;
    }
}
