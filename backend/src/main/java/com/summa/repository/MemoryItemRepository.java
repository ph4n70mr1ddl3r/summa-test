package com.summa.repository;

import com.summa.model.MemoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MemoryItemRepository extends JpaRepository<MemoryItem, String> {
    List<MemoryItem> findByMemberId(String memberId);
    List<MemoryItem> findByWorkspaceId(String workspaceId);
    List<MemoryItem> findByTaintedTrue();
    long countByTier(String tier);
}
