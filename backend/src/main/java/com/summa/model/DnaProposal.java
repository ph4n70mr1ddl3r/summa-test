package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "dna_proposals")
public class DnaProposal {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "kind", nullable = false, length = 20)
    private String kind;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "revision", nullable = false)
    private Integer revision;

    @Column(name = "proposed_by", nullable = false, length = 36)
    private String proposedBy;

    @Column(name = "provenance", nullable = false, columnDefinition = "TEXT")
    private String provenance;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;

    @Column(name = "created_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant reviewedAt;

    @Column(name = "review_by")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant reviewBy;

    @Column(name = "domain_id", length = 36)
    private String domainId;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "open";
        if (revision == null) revision = 1;
        if (provenance == null) provenance = "{}";
        if (payload == null) payload = "{}";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Integer getRevision() { return revision; }
    public void setRevision(Integer revision) { this.revision = revision; }
    public String getProposedBy() { return proposedBy; }
    public void setProposedBy(String proposedBy) { this.proposedBy = proposedBy; }
    public String getProvenance() { return provenance; }
    public void setProvenance(String provenance) { this.provenance = provenance; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public Instant getReviewBy() { return reviewBy; }
    public void setReviewBy(Instant reviewBy) { this.reviewBy = reviewBy; }
    public String getDomainId() { return domainId; }
    public void setDomainId(String domainId) { this.domainId = domainId; }
    public boolean isOpen() { return "open".equals(status); }
    public boolean isPublished() { return "published".equals(status); }
}
