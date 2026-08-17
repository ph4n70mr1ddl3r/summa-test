package com.summa.service;

import com.summa.repository.DnaGoalRepository;
import com.summa.model.DnaGoal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DnaGoalService {
    private final DnaGoalRepository goalRepository;
    private final AuditService auditService;

    public DnaGoalService(DnaGoalRepository goalRepository, AuditService auditService) {
        this.goalRepository = goalRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DnaGoal create(String id, String domainId, String quarter, String statementMd,
                            String owner, String inject, Instant effectiveFrom,
                            Instant effectiveTo, String actor) {
        DnaGoal goal = new DnaGoal();
        goal.setId(id);
        goal.setDomainId(domainId);
        goal.setQuarter(quarter);
        goal.setStatementMd(statementMd != null ? statementMd : "");
        goal.setOwner(owner);
        goal.setInject(inject != null ? inject : "linked");
        goal.setEffectiveFrom(effectiveFrom);
        goal.setEffectiveTo(effectiveTo);
        goal.setStatus("active");

        DnaGoal saved = goalRepository.save(goal);
        auditService.log(actor, "CREATE_GOAL", "dna_goal", id,
            String.format("{\"owner\":\"%s\",\"inject\":\"%s\"}", owner, inject));
        return saved;
    }

    public Optional<DnaGoal> findById(String id) {
        return goalRepository.findById(id);
    }

    public List<DnaGoal> findByDomain(String domainId) {
        return goalRepository.findByDomainId(domainId);
    }

    public List<DnaGoal> findActiveInject(String inject, Instant now) {
        return goalRepository.findActiveInject(inject, now);
    }

    public List<DnaGoal> findAllActiveWindowed(Instant now) {
        return goalRepository.findAllActiveWindowed(now);
    }

    @Transactional
    public DnaGoal updateStatus(String id, String status, String actor) {
        DnaGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found: " + id));

        // Terminal statuses are immutable
        if ("met".equals(goal.getStatus()) || "missed".equals(goal.getStatus()) || "retired".equals(goal.getStatus())) {
            throw new IllegalStateException("Cannot update terminal goal: " + goal.getStatus());
        }

        goal.setStatus(status);
        DnaGoal saved = goalRepository.save(goal);
        auditService.log(actor, "UPDATE_GOAL_STATUS", "dna_goal", id,
            String.format("{\"newStatus\":\"%s\"}", status));
        return saved;
    }

    @Transactional
    public DnaGoal updateWindow(String id, Instant effectiveFrom, Instant effectiveTo, String actor) {
        DnaGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found: " + id));

        if (effectiveFrom != null) goal.setEffectiveFrom(effectiveFrom);
        if (effectiveTo != null) goal.setEffectiveTo(effectiveTo);

        DnaGoal saved = goalRepository.save(goal);
        auditService.log(actor, "UPDATE_GOAL_WINDOW", "dna_goal", id, null);
        return saved;
    }
}
