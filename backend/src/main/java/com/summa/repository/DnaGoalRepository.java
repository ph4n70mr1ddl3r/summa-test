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

    long countByDomainIdAndStatus(String domainId, String status);

    /**
     * DGV-040: the live-set check counts the goals that are still in force — an
     * {@code active} goal whose window has already closed has left the slice
     * (DNC-051) and never blocks an archive.
     */
    @Query("SELECT COUNT(g) FROM DnaGoal g WHERE g.domainId = :domainId AND g.status = 'active' AND (g.effectiveTo IS NULL OR g.effectiveTo > :now)")
    long countLiveByDomainId(@org.springframework.data.repository.query.Param("domainId") String domainId,
                             @org.springframework.data.repository.query.Param("now") Instant now);

    @Query("SELECT g FROM DnaGoal g WHERE g.status = 'active' AND g.inject = :inject AND g.effectiveFrom <= :now AND (g.effectiveTo IS NULL OR g.effectiveTo > :now)")
    List<DnaGoal> findActiveInject(@org.springframework.data.repository.query.Param("inject") String inject,
                                     @org.springframework.data.repository.query.Param("now") Instant now);

    @Query("SELECT g FROM DnaGoal g WHERE g.status = 'active' AND g.effectiveFrom <= :now AND (g.effectiveTo IS NULL OR g.effectiveTo > :now) ORDER BY g.inject DESC, g.id ASC")
    List<DnaGoal> findAllActiveWindowed(@org.springframework.data.repository.query.Param("now") Instant now);

}
