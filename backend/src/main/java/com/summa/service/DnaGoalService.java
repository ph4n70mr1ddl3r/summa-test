package com.summa.service;

import com.summa.repository.DnaGoalRepository;
import com.summa.repository.DnaDomainRepository;
import com.summa.model.DnaGoal;
import com.summa.util.JsonHelpers;
import com.summa.util.KeyedUnionValidator;
import com.summa.util.ScanUtils;
import com.summa.service.SecretsScanner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.summa.exception.EntityNotFoundException;

@Service
public class DnaGoalService {
    private final DnaGoalRepository goalRepository;
    private final DnaDomainRepository domainRepository;
    private final AuditService auditService;
    private final SecretsScanner secretsScanner;

    public DnaGoalService(DnaGoalRepository goalRepository, DnaDomainRepository domainRepository,
                          AuditService auditService, SecretsScanner secretsScanner) {
        this.goalRepository = goalRepository;
        this.domainRepository = domainRepository;
        this.auditService = auditService;
        this.secretsScanner = secretsScanner;
    }

    @Transactional
    public DnaGoal create(String id, String domainId, String quarter, String statementMd,
                            String owner, String inject, Instant effectiveFrom,
                            Instant effectiveTo, String actor) {
        if (domainId != null && !domainId.isBlank()) {
            domainRepository.findById(domainId).orElseThrow(
                () -> new EntityNotFoundException("Domain not found: " + domainId));
        }
        // SEC-030: scan for secrets before writing
        ScanUtils.scanForSecrets(statementMd, actor, "dna_goal", id, secretsScanner, auditService);
        DnaGoal goal = new DnaGoal();
        goal.setId(id);
        goal.setDomainId(domainId);
        goal.setQuarter(quarter);
        goal.setStatementMd(statementMd != null ? statementMd : "");
        KeyedUnionValidator.validate(owner, "owner");
        goal.setOwner(owner);
        goal.setInject(inject != null ? inject : "linked");
        goal.setEffectiveFrom(effectiveFrom);
        goal.setEffectiveTo(effectiveTo);
        goal.setStatus("active");

        DnaGoal saved = goalRepository.save(goal);
        auditService.log(actor, "CREATE_GOAL", "dna_goal", id,
            String.format("{\"owner\":%s,\"inject\":%s}", JsonHelpers.jsonString(owner), JsonHelpers.jsonString(inject)));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<DnaGoal> findById(String id) {
        return goalRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<DnaGoal> findByDomain(String domainId) {
        return goalRepository.findByDomainId(domainId);
    }

    @Transactional(readOnly = true)
    public List<DnaGoal> findActiveInject(String inject, Instant now) {
        return goalRepository.findActiveInject(inject, now);
    }

    @Transactional(readOnly = true)
    public List<DnaGoal> findAllActiveWindowed(Instant now) {
        return goalRepository.findAllActiveWindowed(now);
    }

    @Transactional
    public DnaGoal updateStatus(String id, String status, String actor) {
        DnaGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found: " + id));

        // Terminal statuses are immutable
        if ("met".equals(goal.getStatus()) || "missed".equals(goal.getStatus()) || "retired".equals(goal.getStatus())) {
            throw new IllegalArgumentException("Cannot update terminal goal: " + goal.getStatus());
        }

        // Allowlist: only recognized status transitions are permitted
        if (!"active".equals(status) && !"met".equals(status) && !"missed".equals(status) && !"retired".equals(status)) {
            throw new IllegalArgumentException("Invalid goal status: " + status);
        }

        String oldStatus = goal.getStatus();
        goal.setStatus(status);
        DnaGoal saved = goalRepository.save(goal);
        auditService.log(actor, "UPDATE_GOAL_STATUS", "dna_goal", id,
            String.format("{\"newStatus\":%s,\"previousStatus\":%s}", JsonHelpers.jsonString(status), JsonHelpers.jsonString(oldStatus)));
        return saved;
    }

    @Transactional
    public DnaGoal updateWindow(String id, Instant effectiveFrom, Instant effectiveTo, String actor) {
        DnaGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found: " + id));

        // Schema requires effective_from NOT NULL; rejecting a clear-of-both would violate the constraint.
        if (effectiveFrom == null && effectiveTo == null) {
            throw new IllegalArgumentException("At least one of effectiveFrom or effectiveTo must be provided");
        }
        if (effectiveFrom != null) goal.setEffectiveFrom(effectiveFrom);
        if (effectiveTo != null) goal.setEffectiveTo(effectiveTo);

        DnaGoal saved = goalRepository.save(goal);
        auditService.log(actor, "UPDATE_GOAL_WINDOW", "dna_goal", id, null);
        return saved;
    }
}
