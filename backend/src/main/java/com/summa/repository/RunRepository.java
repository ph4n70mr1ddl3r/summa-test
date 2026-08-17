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
    List<Run> findByWorkspaceId(String workspaceId);
    List<Run> findByStatus(String status);
    
    @Query("SELECT r FROM Run r WHERE r.status = 'running' AND r.startedAt < :before")
    List<Run> findRunningBefore(Instant before);
    
    long countByStatus(String status);
    long countByAgentId(String agentId);
}
