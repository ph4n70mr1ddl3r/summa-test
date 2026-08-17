package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "trigger_firings")
public class TriggerFiring {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "trigger_id", nullable = false, length = 36)
    private String triggerId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "fired_at", nullable = false)
    private Instant firedAt;

    @Column(name = "run_id", length = 36)
    private String runId;

    @PrePersist
    public void prePersist() {
        if (firedAt == null) firedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTriggerId() { return triggerId; }
    public void setTriggerId(String triggerId) { this.triggerId = triggerId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Instant getFiredAt() { return firedAt; }
    public void setFiredAt(Instant firedAt) { this.firedAt = firedAt; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
}
