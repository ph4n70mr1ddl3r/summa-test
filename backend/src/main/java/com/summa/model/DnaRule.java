package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "dna_rules")
public class DnaRule {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "domain_id", nullable = false, length = 36)
    private String domainId;

    @Column(name = "statement_md", nullable = false, columnDefinition = "TEXT")
    private String statementMd;

    @Column(name = "machine_hint", columnDefinition = "TEXT")
    private String machineHint;

    @Column(name = "effective_from", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant effectiveTo;

    @Column(name = "supersedes_id", length = 36)
    private String supersedesId;

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
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDomainId() { return domainId; }
    public void setDomainId(String domainId) { this.domainId = domainId; }
    public String getStatementMd() { return statementMd; }
    public void setStatementMd(String statementMd) { this.statementMd = statementMd; }
    public String getMachineHint() { return machineHint; }
    public void setMachineHint(String machineHint) { this.machineHint = machineHint; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Instant effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getSupersedesId() { return supersedesId; }
    public void setSupersedesId(String supersedesId) { this.supersedesId = supersedesId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public boolean isActive() { return "active".equals(status); }
}
