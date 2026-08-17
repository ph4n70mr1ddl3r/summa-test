package com.summa.service;

import com.summa.repository.InitiativeRepository;
import com.summa.model.Initiative;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class InitiativeService {
    private final InitiativeRepository initiativeRepository;
    private final AuditService auditService;

    public InitiativeService(InitiativeRepository initiativeRepository, AuditService auditService) {
        this.initiativeRepository = initiativeRepository;
        this.auditService = auditService;
    }

    public Initiative create(String id, String title, String sponsor, String lead,
                              String goalRef, String decisionRef, Instant deadline,
                              String dependsOn) {
        Initiative initiative = new Initiative();
        initiative.setId(id);
        initiative.setTitle(title);
        initiative.setSponsor(sponsor);
        initiative.setLead(lead);
        initiative.setGoalRef(goalRef);
        initiative.setDecisionRef(decisionRef);
        initiative.setDeadline(deadline);
        initiative.setDependsOn(dependsOn != null ? dependsOn : "[]");

        Initiative saved = initiativeRepository.save(initiative);
        auditService.log(sponsor, "CREATE", "initiative", id, 
            String.format("{\"title\":\"%s\",\"lead\":\"%s\"}", title, lead));
        return saved;
    }

    public Optional<Initiative> findById(String id) {
        return initiativeRepository.findById(id);
    }

    public List<Initiative> findAll() {
        return initiativeRepository.findAll();
    }

    public List<Initiative> findByStatus(String status) {
        return initiativeRepository.findByStatus(status);
    }

    public Initiative activate(String id, String actor) {
        Initiative initiative = initiativeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Initiative not found: " + id));
        
        if (!"proposed".equals(initiative.getStatus())) {
            throw new IllegalStateException("Cannot activate initiative with status: " + initiative.getStatus());
        }
        
        initiative.setStatus("active");
        Initiative saved = initiativeRepository.save(initiative);
        auditService.log(actor, "ACTIVATE", "initiative", id, null);
        return saved;
    }

    public Initiative pause(String id, String actor) {
        Initiative initiative = initiativeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Initiative not found: " + id));
        
        if (!"active".equals(initiative.getStatus())) {
            throw new IllegalStateException("Cannot pause initiative with status: " + initiative.getStatus());
        }
        
        initiative.setStatus("paused");
        Initiative saved = initiativeRepository.save(initiative);
        auditService.log(actor, "PAUSE", "initiative", id, null);
        return saved;
    }

    public Initiative resume(String id, String actor) {
        Initiative initiative = initiativeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Initiative not found: " + id));
        
        if (!"paused".equals(initiative.getStatus())) {
            throw new IllegalStateException("Cannot resume initiative with status: " + initiative.getStatus());
        }
        
        initiative.setStatus("active");
        Initiative saved = initiativeRepository.save(initiative);
        auditService.log(actor, "RESUME", "initiative", id, null);
        return saved;
    }

    public Initiative close(String id, String actor) {
        Initiative initiative = initiativeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Initiative not found: " + id));
        
        if (!"active".equals(initiative.getStatus()) && !"paused".equals(initiative.getStatus())) {
            throw new IllegalStateException("Cannot close initiative with status: " + initiative.getStatus());
        }
        
        initiative.setStatus("closed");
        initiative.setClosedAt(Instant.now());
        Initiative saved = initiativeRepository.save(initiative);
        auditService.log(actor, "CLOSE", "initiative", id, null);
        return saved;
    }
}
