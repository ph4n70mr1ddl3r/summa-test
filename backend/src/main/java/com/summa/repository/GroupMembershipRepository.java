package com.summa.repository;

import com.summa.model.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GroupMembershipRepository extends JpaRepository<GroupMembership, GroupMembership.GroupMembershipId> {
    List<GroupMembership> findById_GroupId(String groupId);
    List<GroupMembership> findById_MemberId(String memberId);
    List<GroupMembership> findByRemovedAtIsNull();
}
