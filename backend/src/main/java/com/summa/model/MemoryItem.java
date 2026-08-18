package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "memory_items")
public class MemoryItem {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "tier", nullable = false, length = 20)
    private String tier;

    @Column(name = "member_id", length = 36)
    private String memberId;

    @Column(name = "workspace_id", length = 36)
    private String workspaceId;

    @Column(name = "content_md", nullable = false, columnDefinition = "TEXT")
    private String contentMd;

    @Column(name = "provenance", nullable = false, columnDefinition = "TEXT")
    private String provenance;

    @Column(name = "tainted", nullable = false)
    private Boolean tainted;

    @Column(name = "created_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant createdAt;

    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant reviewedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (tainted == null) tainted = false;
        if (provenance == null) provenance = "{}";
        if (contentMd == null) contentMd = "";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getContentMd() { return contentMd; }
    public void setContentMd(String contentMd) { this.contentMd = contentMd; }
    public String getProvenance() { return provenance; }
    public void setProvenance(String provenance) { this.provenance = provenance; }
    public Boolean getTainted() { return tainted; }
    public void setTainted(Boolean tainted) { this.tainted = tainted; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public boolean isTainted() { return Boolean.TRUE.equals(tainted); }
    public boolean isReviewed() { return reviewedBy != null && reviewedAt != null; }
}
