package com.summa.service;

import com.summa.repository.DnaGlossaryRepository;
import com.summa.model.DnaGlossary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class DnaGlossaryService {
    private final DnaGlossaryRepository glossaryRepository;
    private final AuditService auditService;

    public DnaGlossaryService(DnaGlossaryRepository glossaryRepository, AuditService auditService) {
        this.glossaryRepository = glossaryRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DnaGlossary create(String id, String domainId, String term, String definition,
                               String aliases, String actor) {
        // Check for duplicate term
        Optional<DnaGlossary> existing = glossaryRepository.findByTermAndDomainId(term, domainId);
        if (existing.isPresent() && !"retired".equals(existing.get().getStatus())) {
            throw new IllegalArgumentException("Term already exists in this domain: " + term);
        }

        DnaGlossary entry = new DnaGlossary();
        entry.setId(id);
        entry.setDomainId(domainId);
        entry.setTerm(term);
        entry.setDefinition(definition != null ? definition : "");
        entry.setAliases(aliases != null ? aliases : "[]");
        entry.setStatus("active");

        DnaGlossary saved = glossaryRepository.save(entry);
        auditService.log(actor, "CREATE_GLOSSARY", "dna_glossary", id,
            String.format("{\"term\":\"%s\",\"domainId\":\"%s\"}", term, domainId));
        return saved;
    }

    public Optional<DnaGlossary> findById(String id) {
        return glossaryRepository.findById(id);
    }

    public List<DnaGlossary> findByDomain(String domainId) {
        return glossaryRepository.findByDomainId(domainId);
    }

    public List<DnaGlossary> findAllActive() {
        return glossaryRepository.findAll().stream()
                .filter(DnaGlossary::isActive)
                .toList();
    }

    public List<DnaGlossary> findByScope(String domainId) {
        return glossaryRepository.findActiveByScope(domainId);
    }

    @Transactional
    public DnaGlossary retire(String id, String actor) {
        DnaGlossary entry = glossaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Glossary entry not found: " + id));

        entry.setStatus("retired");
        DnaGlossary saved = glossaryRepository.save(entry);
        auditService.log(actor, "RETIRE_GLOSSARY", "dna_glossary", id, null);
        return saved;
    }

    @Transactional
    public DnaGlossary update(String id, String definition, String aliases, String actor) {
        DnaGlossary entry = glossaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Glossary entry not found: " + id));

        if (!"active".equals(entry.getStatus()) && !"draft".equals(entry.getStatus())) {
            throw new IllegalStateException("Cannot update retired entry");
        }

        if (definition != null) entry.setDefinition(definition);
        if (aliases != null) entry.setAliases(aliases);

        DnaGlossary saved = glossaryRepository.save(entry);
        auditService.log(actor, "UPDATE_GLOSSARY", "dna_glossary", id, null);
        return saved;
    }
}
