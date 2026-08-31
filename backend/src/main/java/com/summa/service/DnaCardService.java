package com.summa.service;

import com.summa.repository.DnaCardRepository;
import com.summa.model.DnaCard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DnaCardService {
    private final DnaCardRepository cardRepository;
    private final AuditService auditService;
    private final SecretsScanner secretsScanner;

    public DnaCardService(DnaCardRepository cardRepository, AuditService auditService,
                           SecretsScanner secretsScanner) {
        this.cardRepository = cardRepository;
        this.auditService = auditService;
        this.secretsScanner = secretsScanner;
    }

    @Transactional
    public DnaCard create(String id, String domainId, String title, String definitionMd,
                          String provenance, String actor) {
        scanForSecrets(definitionMd, actor, "dna_card", id);
        DnaCard card = new DnaCard();
        card.setId(id);
        card.setDomainId(domainId);
        card.setTitle(title);
        card.setDefinitionMd(definitionMd != null ? definitionMd : "");
        card.setProvenance(provenance != null ? provenance : "{}");
        card.setStatus("active");

        DnaCard saved = cardRepository.save(card);
        auditService.log(actor, "CREATE_CARD", "dna_card", id,
            String.format("{\"domainId\":\"%s\",\"title\":\"%s\"}", domainId, title));
        return saved;
    }

    public Optional<DnaCard> findById(String id) {
        return cardRepository.findById(id);
    }

    public List<DnaCard> findByDomain(String domainId) {
        return cardRepository.findActiveByDomain(domainId);
    }

    public List<DnaCard> findAllActive() {
        return cardRepository.findAll().stream()
                .filter(DnaCard::isActive)
                .toList();
    }

    @Transactional
    public DnaCard update(String id, String title, String definitionMd, String provenance, String actor) {
        DnaCard card = cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + id));

        if (!"active".equals(card.getStatus()) && !"draft".equals(card.getStatus())) {
            throw new IllegalStateException("Cannot update retired card: " + card.getStatus());
        }

        if (title != null) card.setTitle(title);
        if (definitionMd != null) card.setDefinitionMd(definitionMd);
        if (provenance != null) card.setProvenance(provenance);

        DnaCard saved = cardRepository.save(card);
        auditService.log(actor, "UPDATE_CARD", "dna_card", id, null);
        return saved;
    }

    @Transactional
    public DnaCard retire(String id, String actor) {
        DnaCard card = cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + id));

        card.setStatus("retired");
        DnaCard saved = cardRepository.save(card);
        auditService.log(actor, "RETIRE_CARD", "dna_card", id, null);
        return saved;
    }

    @Transactional
    public DnaCard createDraft(String id, String domainId, String title, String definitionMd,
                                 String provenance, String actor) {
        scanForSecrets(definitionMd, actor, "dna_card", id);
        DnaCard card = new DnaCard();
        card.setId(id);
        card.setDomainId(domainId);
        card.setTitle(title);
        card.setDefinitionMd(definitionMd != null ? definitionMd : "");
        card.setProvenance(provenance != null ? provenance : "{}");
        card.setStatus("draft");

        DnaCard saved = cardRepository.save(card);
        auditService.log(actor, "CREATE_DRAFT", "dna_card", id,
            String.format("{\"domainId\":\"%s\",\"title\":\"%s\"}", domainId, title));
        return saved;
    }

    private void scanForSecrets(String content, String actor, String objectType, String objectId) {
        if (content != null && secretsScanner.hasSecrets(content)) {
            auditService.logSystem("SECRET_DETECTED", objectType, objectId,
                String.format("{\"actor\":\"%s\",\"findings\":[%s]}", actor,
                    secretsScanner.scan(content).stream().map(f -> "\"" + f + "\"").collect(Collectors.joining(","))));
            throw new IllegalStateException("Content contains secrets and cannot be written");
        }
    }
}
