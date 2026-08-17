package com.summa.repository;

import com.summa.model.Trigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TriggerRepository extends JpaRepository<Trigger, String> {
    List<Trigger> findByStatus(String status);
    List<Trigger> findByAgentId(String agentId);
    List<Trigger> findByWorkspaceId(String workspaceId);
}
