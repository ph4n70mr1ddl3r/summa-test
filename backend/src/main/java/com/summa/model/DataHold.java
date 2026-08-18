package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "data_holds")
public class DataHold {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "kind", nullable = false, length = 20)
    private String kind;

    @Column(name = "subject_id", nullable = false, length = 36)
    private String subjectId;

    @Column(name = "reason_md", nullable = false, columnDefinition = "TEXT")
    private String reasonMd;

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant createdAt;

    @Column(name = "released_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant releasedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (reasonMd == null) reasonMd = "";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
    public String getReasonMd() { return reasonMd; }
    public void setReasonMd(String reasonMd) { this.reasonMd = reasonMd; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getReleasedAt() { return releasedAt; }
    public void setReleasedAt(Instant releasedAt) { this.releasedAt = releasedAt; }
    public boolean isActive() { return releasedAt == null; }
}
