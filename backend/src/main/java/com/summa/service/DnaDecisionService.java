package com.summa.service;

import com.summa.repository.DnaDecisionRepository;
import com.summa.model.DnaDecision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DnaDecisionService {
    private static final Pattern KEYED_UNION_PATTERN = Pattern.compile("^[ha]?:.+$|^[a-zA-Z0-9_-]+$");

    private final DnaDecisionRepository decisionRepository;
    private final AuditService auditService;
    private final MemberService memberService;
    private final SecretsScanner secretsScanner;

    public DnaDecisionService(DnaDecisionRepository decisionRepository, AuditService auditService,
                               MemberService memberService, SecretsScanner secretsScanner) {
        this.decisionRepository = decisionRepository;
        this.auditService = auditService;
        this.memberService = memberService;
        this.secretsScanner = secretsScanner;
    }

    @Transactional
    public DnaDecision create(String id, String domainId, String contextMd, String outcomeMd,
                               String decidedBy, String provenance, String actor) {
        validateKeyedUnion(decidedBy, "decidedBy");
        scanForSecrets(contextMd, actor, "dna_decision", id);
        scanForSecrets(outcomeMd, actor, "dna_decision", id);

        DnaDecision decision = new DnaDecision();
        decision.setId(id);
        decision.setDomainId(domainId);
        decision.setContextMd(contextMd != null ? contextMd : "");
        decision.setOutcomeMd(outcomeMd != null ? outcomeMd : "");
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

    private void validateKeyedUnion(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (!KEYED_UNION_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a valid keyed union (h:<human-id> or a:<agent-id>)");
        }
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
