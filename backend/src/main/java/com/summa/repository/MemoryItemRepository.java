package com.summa.repository;

import com.summa.model.MemoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MemoryItemRepository extends JpaRepository<MemoryItem, String> {
    List<MemoryItem> findByMemberId(String memberId);
    List<MemoryItem> findByWorkspaceId(String workspaceId);
    List<MemoryItem> findByTaintedTrue();
    @Query("SELECT m FROM MemoryItem m WHERE m.memberId = :memberId ORDER BY m.createdAt DESC")
    List<MemoryItem> findByMemberId(@Param("memberId") String memberId, @Param("limit") int limit);
    @Query("SELECT m FROM MemoryItem m WHERE m.workspaceId = :workspaceId ORDER BY m.createdAt DESC")
    List<MemoryItem> findByWorkspaceId(@Param("workspaceId") String workspaceId, @Param("limit") int limit);
    @Query("SELECT m FROM MemoryItem m WHERE m.tainted = true ORDER BY m.createdAt DESC")
    List<MemoryItem> findByTaintedTrue(@Param("limit") int limit);
    @Query("SELECT m FROM MemoryItem m ORDER BY m.createdAt DESC")
    List<MemoryItem> findAll(@Param("limit") int limit);
    long countByTier(String tier);
}
