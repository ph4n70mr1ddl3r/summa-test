package com.summa.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "spawn_requests")
public class SpawnRequest {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "requester_id", nullable = false, length = 36)
    private String requesterId;

    @Column(name = "template_id", length = 36)
    private String templateId;

    @Column(name = "custom_role", length = 100)
    private String customRole;

    @Column(name = "class", nullable = false, length = 20)
    @JsonProperty("class")
    private String spawnClass;

    @Column(name = "purpose", nullable = false, columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "workspace_bindings", nullable = false, columnDefinition = "TEXT")
    private String workspaceBindings;

    @Column(name = "scope_ceiling", nullable = false, columnDefinition = "TEXT")
    private String scopeCeiling;

    @Column(name = "budget_cap")
    private Double budgetCap;

    @Column(name = "ttl_hours")
    private Integer ttlHours;

    @Column(name = "requested_by_human_id", length = 36)
    private String requestedByHumanId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "approved_by", length = 36)
    private String approvedBy;

    @Column(name = "approved_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant approvedAt;

    @Column(name = "agent_id", length = 36)
    private String agentId;

    @Column(name = "gate_target", length = 36)
    private String gateTarget;

    @Column(name = "created_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "requested";
        if (spawnClass == null) spawnClass = "ephemeral";
        if (workspaceBindings == null) workspaceBindings = "[]";
        if (scopeCeiling == null) scopeCeiling = "{}";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getCustomRole() { return customRole; }
    public void setCustomRole(String customRole) { this.customRole = customRole; }
    public String getSpawnClass() { return spawnClass; }
    public void setSpawnClass(String spawnClass) { this.spawnClass = spawnClass; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getWorkspaceBindings() { return workspaceBindings; }
    public void setWorkspaceBindings(String workspaceBindings) { this.workspaceBindings = workspaceBindings; }
    public String getScopeCeiling() { return scopeCeiling; }
    public void setScopeCeiling(String scopeCeiling) { this.scopeCeiling = scopeCeiling; }
    public Double getBudgetCap() { return budgetCap; }
    public void setBudgetCap(Double budgetCap) { this.budgetCap = budgetCap; }
    public Integer getTtlHours() { return ttlHours; }
    public void setTtlHours(Integer ttlHours) { this.ttlHours = ttlHours; }
    public String getRequestedByHumanId() { return requestedByHumanId; }
    public void setRequestedByHumanId(String requestedByHumanId) { this.requestedByHumanId = requestedByHumanId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getGateTarget() { return gateTarget; }
    public void setGateTarget(String gateTarget) { this.gateTarget = gateTarget; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public boolean isPending() { return "requested".equals(status); }
    public boolean isApproved() { return "approved".equals(status); }
}
