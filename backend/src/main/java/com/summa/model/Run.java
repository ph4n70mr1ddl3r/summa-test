package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "runs")
public class Run {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "agent_id", nullable = false, length = 36)
    private String agentId;

    @Column(name = "workspace_id", length = 36)
    private String workspaceId;

    @Column(name = "initiative_id", length = 36)
    private String initiativeId;

    @Column(name = "trigger_id", length = 36)
    private String triggerId;

    @Column(name = "playbook_id", length = 36)
    private String playbookId;

    @Column(name = "parent_run_id", length = 36)
    private String parentRunId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "prompt", nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    @Column(name = "artifacts", nullable = false, columnDefinition = "TEXT")
    private String artifacts;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant startedAt;

    @Column(name = "completed_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant completedAt;

    @Column(name = "cost_tokens", nullable = false)
    private Long costTokens;

    @Column(name = "cost_usd", nullable = false)
    private Double costUsd;

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
        if (status == null) status = "queued";
        if (artifacts == null) artifacts = "[]";
        if (costTokens == null) costTokens = 0L;
        if (costUsd == null) costUsd = 0.0;
        if (prompt == null) prompt = "";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getInitiativeId() { return initiativeId; }
    public void setInitiativeId(String initiativeId) { this.initiativeId = initiativeId; }
    public String getTriggerId() { return triggerId; }
    public void setTriggerId(String triggerId) { this.triggerId = triggerId; }
    public String getPlaybookId() { return playbookId; }
    public void setPlaybookId(String playbookId) { this.playbookId = playbookId; }
    public String getParentRunId() { return parentRunId; }
    public void setParentRunId(String parentRunId) { this.parentRunId = parentRunId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getArtifacts() { return artifacts; }
    public void setArtifacts(String artifacts) { this.artifacts = artifacts; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Long getCostTokens() { return costTokens; }
    public void setCostTokens(Long costTokens) { this.costTokens = costTokens; }
    public Double getCostUsd() { return costUsd; }
    public void setCostUsd(Double costUsd) { this.costUsd = costUsd; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public boolean isRunning() { return "running".equals(status); }
    public boolean isTerminal() {
        return "completed".equals(status) || "failed".equals(status) || "cancelled".equals(status);
    }
}
