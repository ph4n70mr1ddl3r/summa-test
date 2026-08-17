package com.summa.repository;

import com.summa.model.SpawnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SpawnRequestRepository extends JpaRepository<SpawnRequest, String> {
    List<SpawnRequest> findByStatus(String status);
    List<SpawnRequest> findByRequesterId(String requesterId);
    List<SpawnRequest> findByAgentId(String agentId);
    long countByStatus(String status);
}
