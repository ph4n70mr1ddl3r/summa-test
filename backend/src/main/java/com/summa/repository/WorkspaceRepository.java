package com.summa.repository;

import com.summa.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, String> {
    List<Workspace> findByNodeId(String nodeId);
    List<Workspace> findByArchivedAtIsNull();
    long countByArchivedAtIsNull();
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(w) FROM Workspace w WHERE w.archivedAt IS NULL AND w.domainIds LIKE %:domainId%")
    long countByDomainIdsContaining(@org.springframework.data.repository.query.Param("domainId") String domainId);
}
