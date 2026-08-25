package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "humans")
public class Human {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "rbac", nullable = false, length = 20)
    private String rbac;

    @Column(name = "auth", nullable = false, columnDefinition = "TEXT")
    private String auth;

    @Column(name = "password_hash", length = 64)
    private String passwordHash;

    @Column(name = "deputy_member_id", length = 36)
    private String deputyMemberId;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "working_hours", columnDefinition = "TEXT")
    private String workingHours;

    @Column(name = "created_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant updatedAt;

    @Column(name = "deactivated_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant deactivatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRbac() { return rbac; }
    public void setRbac(String rbac) { this.rbac = rbac; }
    public String getAuth() { return auth; }
    public void setAuth(String auth) { this.auth = auth; }
    public String getDeputyMemberId() { return deputyMemberId; }
    public void setDeputyMemberId(String deputyMemberId) { this.deputyMemberId = deputyMemberId; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getWorkingHours() { return workingHours; }
    public void setWorkingHours(String workingHours) { this.workingHours = workingHours; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(Instant deactivatedAt) { this.deactivatedAt = deactivatedAt; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isActive() { return deactivatedAt == null; }
}
