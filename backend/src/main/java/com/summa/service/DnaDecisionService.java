package com.summa.service;

import com.summa.repository.DnaDecisionRepository;
import com.summa.repository.DnaDomainRepository;
import com.summa.model.DnaDecision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import com.summa.util.ScanUtils;
import com.summa.util.JsonHelpers;
import com.summa.util.DomainGuards;

@Service
public class DnaDecisionService {
    private final DnaDecisionRepository decisionRepository;
    private final DnaDomainRepository domainRepository;
    private final AuditService auditService;
    private final MemberService memberService;
    private final SecretsScanner secretsScanner;

    public DnaDecisionService(DnaDecisionRepository decisionRepository, DnaDomainRepository domainRepository, AuditService auditService,
                               MemberService memberService, SecretsScanner secretsScanner) {
        this.decisionRepository = decisionRepository;
        this.domainRepository = domainRepository;
        this.auditService = auditService;
        this.memberService = memberService;
        this.secretsScanner = secretsScanner;
    }

    @Transactional
    public DnaDecision create(String id, String domainId, String contextMd, String outcomeMd,
                               String decidedBy, String provenance, String actor) {
        if (domainId != null && !domainId.isBlank()) {
            // DGV-041: an archived domain is read-only history — no new items land in it.
            DomainGuards.requireWritable(domainRepository.findById(domainId).orElse(null), domainId);
        }
        // DAT-120: dna_decisions.decided_by is a keyed union (h:<humans.id> /
        // a:<agents.id>) — resolve the kind by live lookup and store the keyed form.
        String decidedByKeyed = decidedBy != null && !decidedBy.isBlank()
                ? memberService.toKeyedMemberRef(decidedBy)
                : decidedBy;
        if (decidedBy != null && !decidedBy.isBlank() && decidedByKeyed == null) {
            throw new IllegalArgumentException("decidedBy does not reference an existing member: " + decidedBy);
        }
        ScanUtils.scanForSecrets(contextMd, actor, "dna_decision", id, secretsScanner, auditService);
        ScanUtils.scanForSecrets(outcomeMd, actor, "dna_decision", id, secretsScanner, auditService);

        DnaDecision decision = new DnaDecision();
        decision.setId(id);
        decision.setDomainId(domainId);
        decision.setContextMd(contextMd != null ? contextMd : "");
        decision.setOutcomeMd(outcomeMd != null ? outcomeMd : "");
        decision.setDecidedBy(decidedByKeyed);
        decision.setProvenance(provenance != null ? provenance : "{}");

        DnaDecision saved = decisionRepository.save(decision);
        auditService.log(actor, "CREATE_DECISION", "dna_decision", id,
            String.format("{\"domainId\":%s,\"decidedBy\":%s}", JsonHelpers.jsonString(domainId), JsonHelpers.jsonString(decidedByKeyed)));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<DnaDecision> findById(String id) {
        return decisionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<DnaDecision> findByDomain(String domainId) {
        return decisionRepository.findByDomainId(domainId);
    }

    @Transactional(readOnly = true)
    public List<DnaDecision> findAll() {
        return decisionRepository.findAll();
    }
}
