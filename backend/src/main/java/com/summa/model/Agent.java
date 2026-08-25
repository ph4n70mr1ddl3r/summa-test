package com.summa.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "agents")
public class Agent {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "owner_human_id", nullable = false, length = 36)
    private String ownerHumanId;

    @JsonProperty("class")
    @Column(name = "class", nullable = false, length = 20)
    private String agentClass;

    @Column(name = "spawned_by", length = 36)
    private String spawnedBy;

    @Column(name = "ttl_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant ttlAt;

    @Column(name = "budget_cap")
    private Double budgetCap;

    @Column(name = "lineage_depth", nullable = false)
    private Integer lineageDepth;

    @Column(name = "template_id", length = 36)
    private String templateId;

    @Column(name = "template_version", length = 50)
    private String templateVersion;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant createdAt;

    @Column(name = "suspended_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant suspendedAt;

    @Column(name = "retired_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant retiredAt;

    @Column(name = "archived_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant archivedAt;

    @Column(name = "updated_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (lineageDepth == null) lineageDepth = 0;
        if (status == null) status = "requested";
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
    public String getAgentClass() { return agentClass; }
    public void setAgentClass(String agentClass) { this.agentClass = agentClass; }
    public String getSpawnedBy() { return spawnedBy; }
    public void setSpawnedBy(String spawnedBy) { this.spawnedBy = spawnedBy; }
    public Instant getTtlAt() { return ttlAt; }
    public void setTtlAt(Instant ttlAt) { this.ttlAt = ttlAt; }
    public Double getBudgetCap() { return budgetCap; }
    public void setBudgetCap(Double budgetCap) { this.budgetCap = budgetCap; }
    public Integer getLineageDepth() { return lineageDepth; }
    public void setLineageDepth(Integer lineageDepth) { this.lineageDepth = lineageDepth; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String templateVersion) { this.templateVersion = templateVersion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getSuspendedAt() { return suspendedAt; }
    public void setSuspendedAt(Instant suspendedAt) { this.suspendedAt = suspendedAt; }
    public Instant getRetiredAt() { return retiredAt; }
    public void setRetiredAt(Instant retiredAt) { this.retiredAt = retiredAt; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public boolean isActive() { return "active".equals(status); }
    public boolean isEphemeral() { return "ephemeral".equals(agentClass); }
}
