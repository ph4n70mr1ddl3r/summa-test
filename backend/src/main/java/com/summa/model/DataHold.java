package com.summa.model;

import com.summa.config.InstantToUnixEpochConverter;
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
    @Convert(converter = InstantToUnixEpochConverter.class)
    private Instant createdAt;

    @Column(name = "released_at")
    @Convert(converter = InstantToUnixEpochConverter.class)
    private Instant releasedAt;

    // Snapshot of releasedAt taken at load time (@PostLoad); transient, never
    // persisted — used by preUpdate() to distinguish a release from a mutation.
    @Transient
    private Instant originalReleasedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (reasonMd == null) reasonMd = "";
    }

    /**
     * Snapshot the released state on load so {@link #preUpdate()} can tell a
     * legitimate release (null -> non-null releasedAt) apart from any other
     * mutation of an append-only row.
     */
    @PostLoad
    void captureOriginalReleasedAt() {
        this.originalReleasedAt = this.releasedAt;
    }

    @PreUpdate
    public void preUpdate() {
        // data_holds is append-only except for a single release: releasedAt may
        // transition null -> non-null exactly once. Any other mutation, or
        // touching an already-released hold, is refused.
        if (originalReleasedAt == null && releasedAt != null) {
            return;
        }
        throw new UnsupportedOperationException(
                "data_holds is append-only except for a single null->non-null releasedAt release");
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
