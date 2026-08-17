package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "role_templates")
public class RoleTemplate {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "class", nullable = false, length = 30)
    private String agentClass;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "default_scopes", nullable = false, columnDefinition = "TEXT")
    private String defaultScopes;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (version == null) version = 1;
        if (status == null) status = "draft";
        if (body == null) body = "{}";
        if (defaultScopes == null) defaultScopes = "{}";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getAgentClass() { return agentClass; }
    public void setAgentClass(String agentClass) { this.agentClass = agentClass; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getDefaultScopes() { return defaultScopes; }
    public void setDefaultScopes(String defaultScopes) { this.defaultScopes = defaultScopes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public boolean isActive() { return "active".equals(status); }
}
