package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "governance_settings")
public class GovernanceSetting {
    @Id
    @Column(name = "key", length = 100)
    private String key;

    @Column(name = "value", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(name = "edited_by", nullable = false, length = 36)
    private String editedBy;

    @Column(name = "edited_at", nullable = false)
    private Instant editedAt;

    @PrePersist
    public void prePersist() {
        if (editedAt == null) editedAt = Instant.now();
        if (value == null) value = "{}";
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getEditedBy() { return editedBy; }
    public void setEditedBy(String editedBy) { this.editedBy = editedBy; }
    public Instant getEditedAt() { return editedAt; }
    public void setEditedAt(Instant editedAt) { this.editedAt = editedAt; }
}
