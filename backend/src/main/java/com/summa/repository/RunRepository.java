package com.summa.repository;

import com.summa.model.Run;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface RunRepository extends JpaRepository<Run, String> {
    List<Run> findByAgentId(String agentId);
    List<Run> findByAgentIdAndStatus(String agentId, String status);
    List<Run> findByWorkspaceId(String workspaceId);
    List<Run> findByWorkspaceIdAndStatus(String workspaceId, String status);
    List<Run> findByStatus(String status);
    List<Run> findByAgentIdOrderByCreatedAtDesc(String agentId);
    List<Run> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
    List<Run> findByStatusOrderByCreatedAtDesc(String status);
    List<Run> findByInitiativeIdAndStatus(String initiativeId, String status);
    
    @Query("SELECT r FROM Run r WHERE r.status = 'running' AND r.startedAt < :before")
    List<Run> findRunningBefore(Instant before);
    
    long countByStatus(String status);
    long countByAgentId(String agentId);

    @Query("SELECT r FROM Run r ORDER BY r.createdAt DESC")
    List<Run> findByOrderByCreatedAtDesc();
}
