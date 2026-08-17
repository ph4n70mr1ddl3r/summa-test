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
}
