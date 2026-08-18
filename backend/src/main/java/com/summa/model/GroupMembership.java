package com.summa.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "group_memberships")
public class GroupMembership {
    @EmbeddedId
    private GroupMembershipId id;

    @Column(name = "added_by", nullable = false, length = 36)
    private String addedBy;

    @Column(name = "added_at", nullable = false)
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant addedAt;

    @Column(name = "removed_at")
    @Convert(converter = com.summa.config.InstantToUnixEpochConverter.class)
    private Instant removedAt;

    @PrePersist
    public void prePersist() {
        if (addedAt == null) addedAt = Instant.now();
    }

    public GroupMembershipId getId() { return id; }
    public void setId(GroupMembershipId id) { this.id = id; }
    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }
    public Instant getRemovedAt() { return removedAt; }
    public void setRemovedAt(Instant removedAt) { this.removedAt = removedAt; }
    public boolean isActive() { return removedAt == null; }

    @Embeddable
    public static class GroupMembershipId implements java.io.Serializable {
        @Column(name = "group_id", length = 36)
        private String groupId;
        @Column(name = "member_id", length = 36)
        private String memberId;

        public GroupMembershipId() {}
        public GroupMembershipId(String groupId, String memberId) {
            this.groupId = groupId;
            this.memberId = memberId;
        }

        public String getGroupId() { return groupId; }
        public void setGroupId(String groupId) { this.groupId = groupId; }
        public String getMemberId() { return memberId; }
        public void setMemberId(String memberId) { this.memberId = memberId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GroupMembershipId)) return false;
            GroupMembershipId that = (GroupMembershipId) o;
            return java.util.Objects.equals(groupId, that.groupId) &&
                   java.util.Objects.equals(memberId, that.memberId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(groupId, memberId);
        }
    }
}
