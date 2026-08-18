package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "dna_domains")
public class DnaDomain {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "owner_human_id", nullable = false, length = 36)
    private String ownerHumanId;

    @Column(name = "access", nullable = false, length = 20)
    private String access;

    @Column(name = "named_readers", nullable = false, columnDefinition = "TEXT")
    private String namedReaders;

    @Column(name = "store", nullable = false, length = 10)
    private String store;

    @Column(name = "sod", nullable = false, length = 20)
    private String sod;

    @Column(name = "review_sla_days", nullable = false)
    private Integer reviewSlaDays;

    @Column(name = "residency", length = 50)
    private String residency;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (status == null) status = "active";
        if (access == null) access = "public";
        if (store == null) store = "git";
        if (sod == null) sod = "off";
        if (reviewSlaDays == null) reviewSlaDays = 7;
        if (namedReaders == null) namedReaders = "[]";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwnerHumanId() { return ownerHumanId; }
    public void setOwnerHumanId(String ownerHumanId) { this.ownerHumanId = ownerHumanId; }
    public String getAccess() { return access; }
    public void setAccess(String access) { this.access = access; }
    public String getNamedReaders() { return namedReaders; }
    public void setNamedReaders(String namedReaders) { this.namedReaders = namedReaders; }
    public String getStore() { return store; }
    public void setStore(String store) { this.store = store; }
    public String getSod() { return sod; }
    public void setSod(String sod) { this.sod = sod; }
    public Integer getReviewSlaDays() { return reviewSlaDays; }
    public void setReviewSlaDays(Integer reviewSlaDays) { this.reviewSlaDays = reviewSlaDays; }
    public String getResidency() { return residency; }
    public void setResidency(String residency) { this.residency = residency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public boolean isActive() { return "active".equals(status); }
    public boolean isRestricted() { return !"public".equals(access); }
}
