package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pats")
public class Pat {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "member_id", nullable = false, length = 36)
    private String memberId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "scopes", nullable = false, columnDefinition = "TEXT")
    private String scopes;

    @Column(name = "created_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant revokedAt;

    @Column(name = "last_used_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant lastUsedAt;

    @Column(name = "updated_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (scopes == null) scopes = "[]";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public boolean isExpired() { return expiresAt != null && expiresAt.isBefore(Instant.now()); }
    public boolean isRevoked() { return revokedAt != null; }
    public boolean isActive() { return !isExpired() && !isRevoked(); }
}
