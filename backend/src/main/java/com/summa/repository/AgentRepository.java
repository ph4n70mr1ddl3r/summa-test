package com.summa.repository;

import com.summa.model.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AgentRepository extends JpaRepository<Agent, String> {

    @Query("SELECT a FROM Agent a WHERE a.status = 'active'")
    List<Agent> findAllActive();

    @Query("SELECT a FROM Agent a WHERE a.status = 'active' ORDER BY a.createdAt DESC")
    List<Agent> findAllActiveOrdered();

    @Query("SELECT a FROM Agent a WHERE a.status = 'active' ORDER BY a.createdAt DESC LIMIT :limit")
    List<Agent> findAllActiveOrdered(@org.springframework.data.repository.query.Param("limit") int limit);

    @Query("SELECT COUNT(a) FROM Agent a WHERE a.status = 'active'")
    long countActiveAgents();

    List<Agent> findBySpawnedBy(String spawnedBy);

    @Query("SELECT a FROM Agent a WHERE a.ownerHumanId = :ownerId AND a.status = 'active'")
    List<Agent> findActiveByOwner(String ownerId);

    @Query("SELECT a FROM Agent a WHERE a.ownerHumanId = :ownerId")
    List<Agent> findByOwner(String ownerId);

    @Query("SELECT a FROM Agent a WHERE a.ownerHumanId = :ownerId ORDER BY a.createdAt DESC")
    List<Agent> findByOwnerOrdered();

    @Query("SELECT a FROM Agent a WHERE a.ownerHumanId = :ownerId ORDER BY a.createdAt DESC LIMIT :limit")
    List<Agent> findByOwnerOrdered(@org.springframework.data.repository.query.Param("ownerId") String ownerId,
                                   @org.springframework.data.repository.query.Param("limit") int limit);

    @Query("SELECT a FROM Agent a WHERE a.status = :status")
    List<Agent> findByStatus(String status);

    @Query("SELECT a FROM Agent a WHERE a.status = :status ORDER BY a.createdAt DESC LIMIT :limit")
    List<Agent> findByStatusOrdered(@org.springframework.data.repository.query.Param("status") String status,
                                    @org.springframework.data.repository.query.Param("limit") int limit);

    @Query("SELECT a FROM Agent a WHERE a.status = 'active' AND a.ttlAt IS NOT NULL AND a.ttlAt < :now")
    List<Agent> findActiveExpiredBefore(java.time.Instant now);

    @Query("SELECT a FROM Agent a WHERE a.status = 'suspended' AND a.ttlAt IS NOT NULL AND a.ttlAt < :now")
    List<Agent> findSuspendedExpiredBefore(java.time.Instant now);

    @Query("SELECT a FROM Agent a WHERE a.templateId = :templateId AND a.status = 'active'")
    List<Agent> findActiveByTemplateId(String templateId);

    long countByTemplateIdAndStatus(String templateId, String status);
}
