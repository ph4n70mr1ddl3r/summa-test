package com.summa.repository;

import com.summa.model.Run;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface RunRepository extends JpaRepository<Run, String> {
    List<Run> findByAgentIdAndStatus(String agentId, String status);
    List<Run> findByWorkspaceIdAndStatus(String workspaceId, String status);
    @Query("SELECT r FROM Run r WHERE r.workspaceId = :workspaceId AND r.status IN :statuses")
    List<Run> findByWorkspaceIdAndStatusIn(@org.springframework.data.repository.query.Param("workspaceId") String workspaceId,
                                            @org.springframework.data.repository.query.Param("statuses") List<String> statuses);
    List<Run> findByInitiativeIdAndStatus(String initiativeId, String status);
    
    @Query("SELECT r FROM Run r WHERE r.status = 'running' AND r.startedAt < :before")
    List<Run> findRunningBefore(@org.springframework.data.repository.query.Param("before") Instant before);
    
    long countByStatus(String status);
    long countByAgentId(String agentId);

    @Query("SELECT r FROM Run r ORDER BY r.createdAt DESC")
    List<Run> findByOrderByCreatedAtDesc();

    @Query("SELECT r FROM Run r WHERE r.agentId = :agentId ORDER BY r.createdAt DESC LIMIT :limit")
    List<Run> findByAgentIdOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("agentId") String agentId,
                                                   @org.springframework.data.repository.query.Param("limit") int limit);

    @Query("SELECT r FROM Run r WHERE r.workspaceId = :workspaceId ORDER BY r.createdAt DESC LIMIT :limit")
    List<Run> findByWorkspaceIdOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("workspaceId") String workspaceId,
                                                       @org.springframework.data.repository.query.Param("limit") int limit);

    @Query("SELECT r FROM Run r WHERE r.status = :status ORDER BY r.createdAt DESC LIMIT :limit")
    List<Run> findByStatusOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("status") String status,
                                                 @org.springframework.data.repository.query.Param("limit") int limit);

    @Query("SELECT r FROM Run r ORDER BY r.createdAt DESC LIMIT :limit")
    List<Run> findByOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("limit") int limit);
}
