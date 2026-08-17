package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "workspaces")
public class Workspace {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "kind", nullable = false, length = 20)
    private String kind;

    @Column(name = "initiative_ids", nullable = false, columnDefinition = "TEXT")
    private String initiativeIds;

    @Column(name = "domain_ids", nullable = false, columnDefinition = "TEXT")
    private String domainIds;

    @Column(name = "node_id", length = 36)
    private String nodeId;

    @Column(name = "claim_epoch", nullable = false)
    private Integer claimEpoch;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "participants", nullable = false, columnDefinition = "TEXT")
    private String participants;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (kind == null) kind = "project";
        if (initiativeIds == null) initiativeIds = "[]";
        if (domainIds == null) domainIds = "[]";
        if (claimEpoch == null) claimEpoch = 0;
        if (participants == null) participants = "[]";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getInitiativeIds() { return initiativeIds; }
    public void setInitiativeIds(String initiativeIds) { this.initiativeIds = initiativeIds; }
    public String getDomainIds() { return domainIds; }
    public void setDomainIds(String domainIds) { this.domainIds = domainIds; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public Integer getClaimEpoch() { return claimEpoch; }
    public void setClaimEpoch(Integer claimEpoch) { this.claimEpoch = claimEpoch; }
    public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
    public void setLeaseExpiresAt(Instant leaseExpiresAt) { this.leaseExpiresAt = leaseExpiresAt; }
    public String getParticipants() { return participants; }
    public void setParticipants(String participants) { this.participants = participants; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public boolean isArchived() { return archivedAt != null; }
}
