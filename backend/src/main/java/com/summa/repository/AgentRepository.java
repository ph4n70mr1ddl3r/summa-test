package com.summa.repository;

import com.summa.model.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, String> {
    Optional<Agent> findByName(String name);
    
    @Query("SELECT a FROM Agent a WHERE a.status = 'active' AND a.agentClass = :clazz")
    List<Agent> findActiveByClass(String clazz);
    
    @Query("SELECT a FROM Agent a WHERE a.status = 'active'")
    List<Agent> findAllActive();
    
    @Query("SELECT COUNT(a) FROM Agent a WHERE a.status = 'active'")
    long countActiveAgents();
    
    List<Agent> findBySpawnedBy(String spawnedBy);
    
    @Query("SELECT a FROM Agent a WHERE a.ownerHumanId = :ownerId AND a.status = 'active'")
    List<Agent> findActiveByOwner(String ownerId);
    
    @Query("SELECT a FROM Agent a WHERE a.status = :status")
    List<Agent> findByStatus(String status);
    
    long countByStatusAndAgentClass(String status, String agentClass);
}
