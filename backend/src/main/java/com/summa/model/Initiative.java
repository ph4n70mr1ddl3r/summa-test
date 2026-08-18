package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "initiatives")
public class Initiative {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "goal_ref", length = 36)
    private String goalRef;

    @Column(name = "decision_ref", length = 36)
    private String decisionRef;

    @Column(name = "sponsor", nullable = false, length = 36)
    private String sponsor;

    @Column(name = "lead", nullable = false, length = 36)
    private String lead;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "business_budget", columnDefinition = "TEXT")
    private String businessBudget;

    @Column(name = "deadline")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant deadline;

    @Column(name = "closed_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant closedAt;

    @Column(name = "depends_on", nullable = false, columnDefinition = "TEXT")
    private String dependsOn;

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
        if (status == null) status = "proposed";
        if (dependsOn == null) dependsOn = "[]";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getGoalRef() { return goalRef; }
    public void setGoalRef(String goalRef) { this.goalRef = goalRef; }
    public String getDecisionRef() { return decisionRef; }
    public void setDecisionRef(String decisionRef) { this.decisionRef = decisionRef; }
    public String getSponsor() { return sponsor; }
    public void setSponsor(String sponsor) { this.sponsor = sponsor; }
    public String getLead() { return lead; }
    public void setLead(String lead) { this.lead = lead; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBusinessBudget() { return businessBudget; }
    public void setBusinessBudget(String businessBudget) { this.businessBudget = businessBudget; }
    public Instant getDeadline() { return deadline; }
    public void setDeadline(Instant deadline) { this.deadline = deadline; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public String getDependsOn() { return dependsOn; }
    public void setDependsOn(String dependsOn) { this.dependsOn = dependsOn; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public boolean isActive() { return "active".equals(status); }
}
