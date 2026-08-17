package com.summa.repository;

import com.summa.model.DnaGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface DnaGoalRepository extends JpaRepository<DnaGoal, String> {
    List<DnaGoal> findByDomainId(String domainId);
    
    @Query("SELECT g FROM DnaGoal g WHERE g.status = 'active' AND g.inject = :inject AND g.effectiveFrom <= :now AND (g.effectiveTo IS NULL OR g.effectiveTo > :now)")
    List<DnaGoal> findActiveInject(String inject, Instant now);
    
    @Query("SELECT g FROM DnaGoal g WHERE g.status = 'active' AND g.effectiveFrom <= :now AND (g.effectiveTo IS NULL OR g.effectiveTo > :now) ORDER BY g.inject DESC, g.id ASC")
    List<DnaGoal> findAllActiveWindowed(Instant now);
    
    long countByStatus(String status);
}
