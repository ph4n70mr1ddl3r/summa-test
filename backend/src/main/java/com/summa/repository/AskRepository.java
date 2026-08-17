package com.summa.repository;

import com.summa.model.Ask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AskRepository extends JpaRepository<Ask, String> {
    List<Ask> findByStatus(String status);
    
    @Query("SELECT a FROM Ask a WHERE a.to = :to AND a.status = 'pending' ORDER BY a.deadline ASC")
    List<Ask> findByToAndStatusPending(String to);
    
    @Query("SELECT a FROM Ask a WHERE a.slaTier = :tier AND a.status = 'pending' AND a.deadline < :now ORDER BY a.deadline ASC")
    List<Ask> findExpiredByTier(java.time.Instant now, String tier);
    
    long countByStatus(String status);
    
    @Query("SELECT a FROM Ask a WHERE a.status = 'pending' AND a.deadline < :now")
    List<Ask> findExpiredBefore(java.time.Instant now);

    @Query("SELECT a FROM Ask a WHERE a.from = :agentId AND a.status = 'pending'")
    List<Ask> findByFromAndStatusPending(String agentId);
}
