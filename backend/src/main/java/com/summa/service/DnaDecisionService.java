package com.summa.service;

import com.summa.repository.DnaDecisionRepository;
import com.summa.model.DnaDecision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DnaDecisionService {
    private final DnaDecisionRepository decisionRepository;
    private final AuditService auditService;

    public DnaDecisionService(DnaDecisionRepository decisionRepository, AuditService auditService) {
        this.decisionRepository = decisionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DnaDecision create(String id, String domainId, String contextMd, String outcomeMd,
                               String decidedBy, String provenance, String actor) {
        DnaDecision decision = new DnaDecision();
        decision.setId(id);
        decision.setDomainId(domainId);
        decision.setContextMd(contextMd != null ? contextMd : "");
        decision.setOutcomeMd(outcomeMd);
        decision.setDecidedBy(decidedBy);
        decision.setProvenance(provenance != null ? provenance : "{}");

        DnaDecision saved = decisionRepository.save(decision);
        auditService.log(actor, "CREATE_DECISION", "dna_decision", id,
            String.format("{\"domainId\":\"%s\",\"decidedBy\":\"%s\"}", domainId, decidedBy));
        return saved;
    }

    public Optional<DnaDecision> findById(String id) {
        return decisionRepository.findById(id);
    }

    public List<DnaDecision> findByDomain(String domainId) {
        return decisionRepository.findByDomainId(domainId);
    }

    public List<DnaDecision> findAll() {
        return decisionRepository.findAll();
    }
}
