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

    @Query("SELECT r FROM SpawnRequest r WHERE r.status = 'requested' AND r.workspaceBindings LIKE %:escapedWorkspaceId%")
    List<SpawnRequest> findPendingByWorkspaceBinding(@Param("escapedWorkspaceId") String workspaceId);

    @Query("SELECT r FROM SpawnRequest r WHERE r.status = :status AND r.workspaceBindings LIKE %:escapedInitiativeId%")
    List<SpawnRequest> findByStatusAndWorkspaceBindingsContaining(@Param("status") String status, @Param("escapedInitiativeId") String initiativeId);

    long countByTemplateIdAndStatus(String templateId, String status);

    /**
     * Escape SQL LIKE wildcards (% and _) so the query treats them as literals.
     * Workspace/Initiative IDs are UUIDs but the guard prevents future format changes from breaking the query.
     */
    static String escapeLike(String value) {
        if (value == null) return null;
        return value.replace("/", "//").replace("%", "/%").replace("_", "/_");
    }
}
