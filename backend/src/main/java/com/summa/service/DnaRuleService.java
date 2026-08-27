package com.summa.service;

import com.summa.repository.DnaRuleRepository;
import com.summa.model.DnaRule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DnaRuleService {
    private final DnaRuleRepository ruleRepository;
    private final AuditService auditService;
    private final DnaDomainService domainService;

    public DnaRuleService(DnaRuleRepository ruleRepository, AuditService auditService,
                          DnaDomainService domainService) {
        this.ruleRepository = ruleRepository;
        this.auditService = auditService;
        this.domainService = domainService;
    }

    @Transactional
    public DnaRule create(String id, String domainId, String statementMd, String machineHint,
                          Instant effectiveFrom, Instant effectiveTo, String supersedesId,
                          String actor) {
        // Validate effective date ordering
        if (effectiveTo != null && effectiveFrom != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveTo must not be before effectiveFrom");
        }

        // Validate supersedes_id is in same domain
        if (supersedesId != null) {
            ruleRepository.findById(supersedesId).ifPresentOrElse(
                existing -> {
                    if (!existing.getDomainId().equals(domainId)) {
                        throw new IllegalArgumentException("Supersedes rule must be in the same domain");
                    }
                    if (!"active".equals(existing.getStatus()) && !"superseded".equals(existing.getStatus())) {
                        throw new IllegalArgumentException("Cannot supersede a lapsed rule");
                    }
                },
                () -> { /* supersedesId does not exist — allow it as a forward reference; the FK handles enforcement */ }
            );

            // Check for forked supersession
            long successorCount = ruleRepository.findBySupersedesId(supersedesId).size();
            if (successorCount > 0) {
                throw new IllegalArgumentException(
                    "Rule already has a superseder. Name the chain's live head instead.");
            }
        }

        DnaRule rule = new DnaRule();
        rule.setId(id);
        rule.setDomainId(domainId);
        rule.setStatementMd(statementMd);
        rule.setMachineHint(machineHint);
        rule.setEffectiveFrom(effectiveFrom);
        rule.setEffectiveTo(effectiveTo);
        rule.setSupersedesId(supersedesId);
        rule.setStatus("active");

        DnaRule saved = ruleRepository.save(rule);
        auditService.log(actor, "CREATE_RULE", "dna_rule", id,
            String.format("{\"domainId\":\"%s\",\"effectiveFrom\":\"%s\"}", domainId, effectiveFrom));
        return saved;
    }

    public Optional<DnaRule> findById(String id) {
        return ruleRepository.findById(id);
    }

    public List<DnaRule> findByDomain(String domainId) {
        return ruleRepository.findByDomainId(domainId);
    }

    public List<DnaRule> findActiveWindowed(String domainId, Instant now) {
        return ruleRepository.findActiveWindowed(domainId, now);
    }

    public List<DnaRule> findAllActiveWindowed(Instant now) {
        return ruleRepository.findAllActiveWindowed(now);
    }

    @Transactional
    public DnaRule update(String id, String statementMd, String machineHint,
                          Instant effectiveTo, String actor) {
        DnaRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + id));

        if (!"active".equals(rule.getStatus())) {
            throw new IllegalStateException("Cannot update non-active rule: " + rule.getStatus());
        }

        if (statementMd != null) rule.setStatementMd(statementMd);
        if (machineHint != null) rule.setMachineHint(machineHint);
        if (effectiveTo != null) {
            rule.setEffectiveTo(effectiveTo);
            if (effectiveTo.isBefore(Instant.now())) {
                rule.setStatus("lapsed");
            }
        }

        DnaRule saved = ruleRepository.save(rule);
        auditService.log(actor, "UPDATE_RULE", "dna_rule", id, null);
        return saved;
    }

    @Transactional
    public DnaRule supersede(String id, String supersedesId, String actor) {
        DnaRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + id));

        DnaRule predecessor = ruleRepository.findById(supersedesId)
                .orElseThrow(() -> new IllegalArgumentException("Predecessor rule not found: " + supersedesId));

        if (!predecessor.getDomainId().equals(rule.getDomainId())) {
            throw new IllegalArgumentException("Cross-domain supersession not allowed");
        }

        predecessor.setStatus("superseded");
        rule.setSupersedesId(supersedesId);

        // Save successor first, then predecessor, within the same transaction
        // If predecessor save fails, the transaction rolls back entirely
        ruleRepository.save(rule);
        DnaRule saved = ruleRepository.save(predecessor);
        auditService.log(actor, "SUPERSEDE_RULE", "dna_rule", id,
            String.format("{\"supersedes\":\"%s\"}", supersedesId));
        return saved;
    }
}
