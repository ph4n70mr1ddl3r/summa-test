package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "asks")
public class Ask {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "kind", nullable = false, length = 20)
    private String kind;

    @Column(name = "from", nullable = false, length = 36)
    private String from;

    @Column(name = "to", nullable = false, length = 36)
    private String to;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "initiative_id", length = 36)
    private String initiativeId;

    @Column(name = "workspace_id", length = 36)
    private String workspaceId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "deadline", nullable = false)
    private Instant deadline;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sla_tier", nullable = false, length = 20)
    private String slaTier;

    @Column(name = "escalation", columnDefinition = "TEXT")
    private String escalation;

    @Column(name = "expiry_behavior", nullable = false, length = 20)
    private String expiryBehavior;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "quorum_required", nullable = false)
    private Integer quorumRequired;

    @Column(name = "responses", nullable = false, columnDefinition = "TEXT")
    private String responses;

    @Column(name = "collapsed_count", nullable = false)
    private Integer collapsedCount;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "pending";
        if (slaTier == null) slaTier = "standard";
        if (expiryBehavior == null) expiryBehavior = "deny";
        if (quorumRequired == null) quorumRequired = 1;
        if (responses == null) responses = "[]";
        if (collapsedCount == null) collapsedCount = 1;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getInitiativeId() { return initiativeId; }
    public void setInitiativeId(String initiativeId) { this.initiativeId = initiativeId; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getDeadline() { return deadline; }
    public void setDeadline(Instant deadline) { this.deadline = deadline; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getSlaTier() { return slaTier; }
    public void setSlaTier(String slaTier) { this.slaTier = slaTier; }
    public String getEscalation() { return escalation; }
    public void setEscalation(String escalation) { this.escalation = escalation; }
    public String getExpiryBehavior() { return expiryBehavior; }
    public void setExpiryBehavior(String expiryBehavior) { this.expiryBehavior = expiryBehavior; }
    public Instant getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }
    public Integer getQuorumRequired() { return quorumRequired; }
    public void setQuorumRequired(Integer quorumRequired) { this.quorumRequired = quorumRequired; }
    public String getResponses() { return responses; }
    public void setResponses(String responses) { this.responses = responses; }
    public Integer getCollapsedCount() { return collapsedCount; }
    public void setCollapsedCount(Integer collapsedCount) { this.collapsedCount = collapsedCount; }
    public boolean isPending() { return "pending".equals(status); }
    public boolean isExpired() { return Instant.now().isAfter(deadline); }
}
