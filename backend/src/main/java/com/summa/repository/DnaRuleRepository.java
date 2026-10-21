package com.summa.repository;

import com.summa.model.DnaRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface DnaRuleRepository extends JpaRepository<DnaRule, String> {
    List<DnaRule> findByDomainId(String domainId);
    
    @Query("SELECT r FROM DnaRule r WHERE r.domainId = :domainId AND r.status = 'active' AND r.effectiveFrom <= :now AND (r.effectiveTo IS NULL OR r.effectiveTo > :now) ORDER BY r.id ASC")
    List<DnaRule> findActiveWindowed(@org.springframework.data.repository.query.Param("domainId") String domainId,
                                      @org.springframework.data.repository.query.Param("now") Instant now);

    @Query("SELECT r FROM DnaRule r WHERE r.status = 'active' AND r.effectiveFrom <= :now AND (r.effectiveTo IS NULL OR r.effectiveTo > :now) ORDER BY r.id ASC")
    List<DnaRule> findAllActiveWindowed(@org.springframework.data.repository.query.Param("now") Instant now);
    
    List<DnaRule> findBySupersedesId(String supersedesId);
    long countByDomainIdAndStatus(String domainId, String status);

    List<DnaRule> findByStatus(String status);

    /**
     * DGV-040: the live-set check counts the rules that are still in force — an
     * {@code active} rule whose window has already closed is expired history, not
     * live state, and never blocks an archive.
     */
    @Query("SELECT COUNT(r) FROM DnaRule r WHERE r.domainId = :domainId AND r.status = 'active' AND (r.effectiveTo IS NULL OR r.effectiveTo > :now)")
    long countLiveByDomainId(@org.springframework.data.repository.query.Param("domainId") String domainId,
                             @org.springframework.data.repository.query.Param("now") Instant now);
}
