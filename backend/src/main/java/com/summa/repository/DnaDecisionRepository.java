package com.summa.repository;

import com.summa.model.DnaDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DnaDecisionRepository extends JpaRepository<DnaDecision, String> {
    List<DnaDecision> findByDomainId(String domainId);

    /**
     * DNC-032: a decision "moves with the corpus on merge, ids stable". The move is
     * a topology re-point, not a content update — DNC-030's immutability covers the
     * decision's own fields, and the entity's @PreUpdate guard refuses any write.
     * The native update touches the domain pointer only and leaves the content
     * columns untouched, so the immutability guarantee holds while the corpus move
     * becomes expressible.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE dna_decisions SET domain_id = :domainId WHERE id = :id", nativeQuery = true)
    int updateDomainId(@Param("id") String id, @Param("domainId") String domainId);
}
