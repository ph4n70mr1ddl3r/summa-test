package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "board_tasks")
public class BoardTask {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "assignee_member_id", length = 36)
    private String assigneeMemberId;

    @Column(name = "initiative_id", length = 36)
    private String initiativeId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "due_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant dueAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant createdAt;

    @Column(name = "completed_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant completedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "open";
        if (priority == null) priority = 0;
        if (description == null) description = "";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAssigneeMemberId() { return assigneeMemberId; }
    public void setAssigneeMemberId(String assigneeMemberId) { this.assigneeMemberId = assigneeMemberId; }
    public String getInitiativeId() { return initiativeId; }
    public void setInitiativeId(String initiativeId) { this.initiativeId = initiativeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public boolean isDone() { return "done".equals(status); }
}
