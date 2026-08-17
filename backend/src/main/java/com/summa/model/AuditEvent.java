package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "at", nullable = false)
    private Instant at;

    @Column(name = "actor", nullable = false, length = 36)
    private String actor;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "object_type", nullable = false, length = 50)
    private String objectType;

    @Column(name = "object_id", nullable = false, length = 36)
    private String objectId;

    @Column(name = "detail", nullable = false, columnDefinition = "TEXT")
    private String detail;

    @Column(name = "node_id", length = 36)
    private String nodeId;

    @Column(name = "origin", nullable = false, length = 10)
    private String origin;

    @PrePersist
    public void prePersist() {
        if (at == null) at = Instant.now();
        if (origin == null) origin = "live";
        if (detail == null) detail = "{}";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Instant getAt() { return at; }
    public void setAt(Instant at) { this.at = at; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getObjectType() { return objectType; }
    public void setObjectType(String objectType) { this.objectType = objectType; }
    public String getObjectId() { return objectId; }
    public void setObjectId(String objectId) { this.objectId = objectId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
}
