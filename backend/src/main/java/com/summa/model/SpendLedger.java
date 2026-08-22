package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "spend_ledger")
public class SpendLedger {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "member_id", nullable = false, length = 36)
    private String memberId;

    @Column(name = "run_id", length = 36)
    private String runId;

    @Column(name = "spawn_id", length = 36)
    private String spawnId;

    @Column(name = "kind", nullable = false, length = 20)
    private String kind;

    @Column(name = "tokens_in", nullable = false)
    private Double tokensIn;

    @Column(name = "tokens_out", nullable = false)
    private Double tokensOut;

    @Column(name = "cost", nullable = false)
    private Double cost;

    @Column(name = "pricing_version", nullable = false, length = 20)
    private String pricingVersion;

    @Column(name = "at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant at;

    @Column(name = "created_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (at == null) at = Instant.now();
        if (createdAt == null) createdAt = Instant.now();
        if (tokensIn == null) tokensIn = 0.0;
        if (tokensOut == null) tokensOut = 0.0;
        if (cost == null) cost = 0.0;
        if (pricingVersion == null) pricingVersion = "v1";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getSpawnId() { return spawnId; }
    public void setSpawnId(String spawnId) { this.spawnId = spawnId; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public Double getTokensIn() { return tokensIn; }
    public void setTokensIn(Double tokensIn) { this.tokensIn = tokensIn; }
    public Double getTokensOut() { return tokensOut; }
    public void setTokensOut(Double tokensOut) { this.tokensOut = tokensOut; }
    public Double getCost() { return cost; }
    public void setCost(Double cost) { this.cost = cost; }
    public String getPricingVersion() { return pricingVersion; }
    public void setPricingVersion(String pricingVersion) { this.pricingVersion = pricingVersion; }
    public Instant getAt() { return at; }
    public void setAt(Instant at) { this.at = at; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
