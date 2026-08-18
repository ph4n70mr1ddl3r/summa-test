package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "external_writes")
public class ExternalWrite {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "run_id", nullable = false, length = 36)
    private String runId;

    @Column(name = "connector", nullable = false, length = 50)
    private String connector;

    @Column(name = "op", nullable = false, length = 50)
    private String op;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "prepared_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant preparedAt;

    @Column(name = "resolved_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant resolvedAt;

    @PrePersist
    public void prePersist() {
        if (preparedAt == null) preparedAt = Instant.now();
        if (status == null) status = "prepared";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getConnector() { return connector; }
    public void setConnector(String connector) { this.connector = connector; }
    public String getOp() { return op; }
    public void setOp(String op) { this.op = op; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getPreparedAt() { return preparedAt; }
    public void setPreparedAt(Instant preparedAt) { this.preparedAt = preparedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public boolean isPrepared() { return "prepared".equals(status); }
    public boolean isCommitted() { return "committed".equals(status); }
    public boolean isFailed() { return "failed".equals(status) || "compensated".equals(status); }
}
