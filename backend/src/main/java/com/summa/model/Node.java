package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "nodes")
public class Node {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "kind", nullable = false, length = 10)
    private String kind;

    @Column(name = "capabilities", nullable = false, columnDefinition = "TEXT")
    private String capabilities;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "claim", columnDefinition = "TEXT")
    private String claim;

    @Column(name = "last_heartbeat")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant lastHeartbeat;

    @Column(name = "pubkey", nullable = false, length = 512)
    private String pubkey;

    @Column(name = "enrolled_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant enrolledAt;

    @Column(name = "revoked_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant revokedAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "updated_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (enrolledAt == null) enrolledAt = Instant.now();
        if (status == null) status = "trusted";
        if (capabilities == null) capabilities = "{}";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getCapabilities() { return capabilities; }
    public void setCapabilities(String capabilities) { this.capabilities = capabilities; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getClaim() { return claim; }
    public void setClaim(String claim) { this.claim = claim; }
    public Instant getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Instant lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    public String getPubkey() { return pubkey; }
    public void setPubkey(String pubkey) { this.pubkey = pubkey; }
    public Instant getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(Instant enrolledAt) { this.enrolledAt = enrolledAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public boolean isTrusted() { return "trusted".equals(status); }
    public boolean isRevoked() { return "revoked".equals(status); }
}
