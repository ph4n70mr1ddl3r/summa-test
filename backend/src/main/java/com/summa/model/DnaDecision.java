package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "dna_decisions")
public class DnaDecision {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "domain_id", nullable = false, length = 36)
    private String domainId;

    @Column(name = "context_md", nullable = false, columnDefinition = "TEXT")
    private String contextMd;

    @Column(name = "outcome_md", nullable = false, columnDefinition = "TEXT")
    private String outcomeMd;

    @Column(name = "decided_by", nullable = false, length = 36)
    private String decidedBy;

    @Column(name = "decided_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant decidedAt;

    @Column(name = "refs", columnDefinition = "TEXT")
    private String refs;

    @Column(name = "provenance", columnDefinition = "TEXT")
    private String provenance;

    @PrePersist
    public void prePersist() {
        if (decidedAt == null) decidedAt = Instant.now();
        if (refs == null) refs = "[]";
        if (provenance == null) provenance = "{}";
        if (contextMd == null) contextMd = "";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDomainId() { return domainId; }
    public void setDomainId(String domainId) { this.domainId = domainId; }
    public String getContextMd() { return contextMd; }
    public void setContextMd(String contextMd) { this.contextMd = contextMd; }
    public String getOutcomeMd() { return outcomeMd; }
    public void setOutcomeMd(String outcomeMd) { this.outcomeMd = outcomeMd; }
    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
    public String getRefs() { return refs; }
    public void setRefs(String refs) { this.refs = refs; }
    public String getProvenance() { return provenance; }
    public void setProvenance(String provenance) { this.provenance = provenance; }
}
