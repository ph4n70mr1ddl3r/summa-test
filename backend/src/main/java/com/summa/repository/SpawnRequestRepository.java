package com.summa.repository;

import com.summa.model.SpawnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SpawnRequestRepository extends JpaRepository<SpawnRequest, String> {
    List<SpawnRequest> findByStatus(String status);
    List<SpawnRequest> findByRequesterId(String requesterId);
    List<SpawnRequest> findByAgentId(String agentId);
    long countByStatus(String status);

    @Query("SELECT r FROM SpawnRequest r WHERE r.status = 'requested' AND r.workspaceBindings LIKE %:workspaceId%")
    List<SpawnRequest> findPendingByWorkspaceBinding(@Param("workspaceId") String workspaceId);

    @Query("SELECT r FROM SpawnRequest r WHERE r.status = :status AND r.workspaceBindings LIKE %:initiativeId%")
    List<SpawnRequest> findByStatusAndWorkspaceBindingsContaining(@Param("status") String status, @Param("initiativeId") String initiativeId);

    long countByTemplateIdAndStatus(String templateId, String status);
}
