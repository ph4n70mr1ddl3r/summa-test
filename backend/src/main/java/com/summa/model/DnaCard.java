package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "dna_cards")
public class DnaCard {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "domain_id", nullable = false, length = 36)
    private String domainId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "definition_md", nullable = false, columnDefinition = "TEXT")
    private String definitionMd;

    @Column(name = "refs", nullable = false, columnDefinition = "TEXT")
    private String refs;

    @Column(name = "provenance", nullable = false, columnDefinition = "TEXT")
    private String provenance;

    @Column(name = "version", nullable = false)
    private Integer version;

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
        if (status == null) status = "active";
        if (refs == null) refs = "[]";
        if (provenance == null) provenance = "{}";
        if (definitionMd == null) definitionMd = "";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        version++;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDomainId() { return domainId; }
    public void setDomainId(String domainId) { this.domainId = domainId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDefinitionMd() { return definitionMd; }
    public void setDefinitionMd(String definitionMd) { this.definitionMd = definitionMd; }
    public String getRefs() { return refs; }
    public void setRefs(String refs) { this.refs = refs; }
    public String getProvenance() { return provenance; }
    public void setProvenance(String provenance) { this.provenance = provenance; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public boolean isActive() { return "active".equals(status); }
}
