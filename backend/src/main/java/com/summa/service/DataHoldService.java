package com.summa.service;

import com.summa.repository.DataHoldRepository;
import com.summa.model.DataHold;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DataHoldService {
    private final DataHoldRepository holdRepository;
    private final AuditService auditService;

    public DataHoldService(DataHoldRepository holdRepository, AuditService auditService) {
        this.holdRepository = holdRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DataHold create(String kind, String subjectId, String reasonMd, String createdBy) {
        DataHold hold = new DataHold();
        hold.setId(UUID.randomUUID().toString());
        hold.setKind(kind);
        hold.setSubjectId(subjectId);
        hold.setReasonMd(reasonMd != null ? reasonMd : "");
        hold.setCreatedBy(createdBy);

        DataHold saved = holdRepository.save(hold);
        auditService.log(createdBy, "CREATE_HOLD", "data_hold", saved.getId(),
            String.format("{\"kind\":\"%s\",\"subjectId\":\"%s\"}", kind, subjectId));
        return saved;
    }

    public Optional<DataHold> findById(String id) {
        return holdRepository.findById(id);
    }

    public List<DataHold> findAllActive() {
        return holdRepository.findByReleasedAtIsNull();
    }

    public List<DataHold> findBySubject(String kind, String subjectId) {
        return holdRepository.findByKindAndSubjectIdAndReleasedAtIsNull(kind, subjectId);
    }

    public boolean hasActiveHold(String kind, String subjectId) {
        return holdRepository.existsByKindAndSubjectIdAndReleasedAtIsNull(kind, subjectId);
    }

    @Transactional
    public DataHold release(String id, String actor) {
        DataHold hold = holdRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Hold not found: " + id));

        hold.setReleasedAt(Instant.now());
        DataHold saved = holdRepository.save(hold);
        auditService.log(actor, "RELEASE_HOLD", "data_hold", id, null);
        return saved;
    }
}
