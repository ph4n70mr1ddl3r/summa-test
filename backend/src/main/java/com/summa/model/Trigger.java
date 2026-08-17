package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "triggers")
public class Trigger {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "kind", nullable = false, length = 20)
    private String kind;

    @Column(name = "expression", nullable = false, columnDefinition = "TEXT")
    private String expression;

    @Column(name = "agent_id", nullable = false, length = 36)
    private String agentId;

    @Column(name = "workspace_id", length = 36)
    private String workspaceId;

    @Column(name = "criticality", nullable = false, length = 20)
    private String criticality;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "config", nullable = false, columnDefinition = "TEXT")
    private String config;

    @Column(name = "last_fired_at")
    private Instant lastFiredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (status == null) status = "active";
        if (criticality == null) criticality = "standard";
        if (expression == null) expression = "";
        if (config == null) config = "{}";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getCriticality() { return criticality; }
    public void setCriticality(String criticality) { this.criticality = criticality; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
    public Instant getLastFiredAt() { return lastFiredAt; }
    public void setLastFiredAt(Instant lastFiredAt) { this.lastFiredAt = lastFiredAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public boolean isActive() { return "active".equals(status); }
}
