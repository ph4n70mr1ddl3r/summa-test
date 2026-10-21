package com.summa.service;

import com.summa.repository.DnaGlossaryRepository;
import com.summa.repository.DnaDomainRepository;
import com.summa.model.DnaGlossary;
import com.summa.exception.EntityNotFoundException;
import com.summa.util.JsonHelpers;
import com.summa.util.ScanUtils;
import com.summa.util.DomainGuards;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class DnaGlossaryService {
    private final DnaGlossaryRepository glossaryRepository;
    private final DnaDomainRepository domainRepository;
    private final AuditService auditService;
    private final SecretsScanner secretsScanner;

    public DnaGlossaryService(DnaGlossaryRepository glossaryRepository, DnaDomainRepository domainRepository,
                              AuditService auditService, SecretsScanner secretsScanner) {
        this.glossaryRepository = glossaryRepository;
        this.domainRepository = domainRepository;
        this.auditService = auditService;
        this.secretsScanner = secretsScanner;
    }

    @Transactional
    public DnaGlossary create(String id, String domainId, String term, String definition,
                               String aliases, String actor) {
        if (domainId != null && !domainId.isBlank()) {
            // DGV-041: an archived domain is read-only history — no new items.
            DomainGuards.requireWritable(domainRepository.findById(domainId).orElse(null), domainId);
        }
        // DNC-041/042: the live entry of the duplicate check is any non-retired row of
        // the same scope; a retired entry no longer blocks the term (retirement is
        // what frees it for reuse).
        List<DnaGlossary> liveEntries = glossaryRepository.findLiveByTermAndScope(term, domainId);
        if (!liveEntries.isEmpty()) {
            throw new IllegalArgumentException("Term already exists in this domain: " + term);
        }
        // DRP-032: the duplicate check covers an alias as well as a term — "term or
        // alias duplicating a live entry of the same scope". The new entry's term
        // and aliases are matched against every live name the scope already holds.
        List<String> claimedNames = new java.util.ArrayList<>();
        claimedNames.add(term);
        claimedNames.addAll(aliasNames(aliases));
        for (DnaGlossary live : glossaryRepository.findLiveByScope(domainId)) {
            for (String liveName : liveNames(live)) {
                for (String claimed : claimedNames) {
                    if (claimed.equalsIgnoreCase(liveName)) {
                        throw new IllegalArgumentException(
                            "Term or alias already exists in this domain: " + claimed);
                    }
                }
            }
        }

        // SEC-030: scan for secrets before writing
        ScanUtils.scanForSecrets(term, actor, "dna_glossary", id, secretsScanner, auditService);
        ScanUtils.scanForSecrets(definition, actor, "dna_glossary", id, secretsScanner, auditService);

        DnaGlossary entry = new DnaGlossary();
        entry.setId(id);
        entry.setDomainId(domainId);
        entry.setTerm(term);
        entry.setDefinition(definition != null ? definition : "");
        entry.setAliases(aliases != null ? aliases : "[]");
        entry.setStatus("active");

        DnaGlossary saved = glossaryRepository.save(entry);
        auditService.log(actor, "CREATE_GLOSSARY", "dna_glossary", id,
            String.format("{\"term\":%s,\"domainId\":%s}", JsonHelpers.jsonString(term), JsonHelpers.jsonString(domainId)));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<DnaGlossary> findById(String id) {
        return glossaryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<DnaGlossary> findByDomain(String domainId) {
        return glossaryRepository.findByDomainId(domainId);
    }

    @Transactional(readOnly = true)
    public List<DnaGlossary> findAllActive() {
        return glossaryRepository.findByStatus("active");
    }

    @Transactional(readOnly = true)
    public List<DnaGlossary> findByScope(String domainId) {
        return glossaryRepository.findActiveByScope(domainId);
    }

    /**
     * DRP-032: the names a live glossary entry holds — its term and its aliases.
     * A malformed alias list contributes only the term, so a legacy row with an
     * unreadable alias column never hides the entry's term from the duplicate
     * check.
     */
    private List<String> liveNames(DnaGlossary entry) {
        List<String> names = new java.util.ArrayList<>();
        if (entry.getTerm() != null && !entry.getTerm().isBlank()) {
            names.add(entry.getTerm());
        }
        names.addAll(aliasNames(entry.getAliases()));
        return names;
    }

    /**
     * The aliases a row holds. The column's canonical spelling is a JSON array;
     * the historical spelling is a comma-separated list, and both are read.
     */
    private List<String> aliasNames(String aliases) {
        if (aliases == null || aliases.isBlank()) return List.of();
        try {
            return JsonHelpers.parseStringList(aliases);
        } catch (IllegalArgumentException e) {
            return java.util.Arrays.stream(aliases.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        }
    }

    @Transactional
    public DnaGlossary retire(String id, String actor) {
        DnaGlossary entry = glossaryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Glossary entry not found: " + id));

        entry.setStatus("retired");
        DnaGlossary saved = glossaryRepository.save(entry);
        auditService.log(actor, "RETIRE_GLOSSARY", "dna_glossary", id, null);
        return saved;
    }

    @Transactional
    public DnaGlossary update(String id, String definition, String aliases, String actor) {
        DnaGlossary entry = glossaryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Glossary entry not found: " + id));

        if (!"active".equals(entry.getStatus()) && !"draft".equals(entry.getStatus())) {
            throw new IllegalArgumentException("Cannot update retired entry");
        }

        if (definition != null) {
            ScanUtils.scanForSecrets(definition, actor, "dna_glossary", id, secretsScanner, auditService);
            entry.setDefinition(definition);
        }
        if (aliases != null) {
            ScanUtils.scanForSecrets(aliases, actor, "dna_glossary", id, secretsScanner, auditService);
            // DRP-032: an alias change is a write into the scope's name space —
            // it must not collide with a live entry of the same scope. The entry
            // being edited is not a collision with itself.
            List<String> claimedNames = new java.util.ArrayList<>();
            claimedNames.add(entry.getTerm());
            claimedNames.addAll(aliasNames(aliases));
            for (DnaGlossary live : glossaryRepository.findLiveByScope(entry.getDomainId())) {
                if (live.getId() != null && live.getId().equals(entry.getId())) continue;
                for (String liveName : liveNames(live)) {
                    for (String claimed : claimedNames) {
                        if (claimed.equalsIgnoreCase(liveName)) {
                            throw new IllegalArgumentException(
                                "Term or alias already exists in this domain: " + claimed);
                        }
                    }
                }
            }
            entry.setAliases(aliases);
        }

        DnaGlossary saved = glossaryRepository.save(entry);
        auditService.log(actor, "UPDATE_GLOSSARY", "dna_glossary", id, null);
        return saved;
    }
}
