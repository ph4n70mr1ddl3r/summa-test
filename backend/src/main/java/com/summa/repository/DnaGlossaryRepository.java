package com.summa.repository;

import com.summa.model.DnaGlossary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DnaGlossaryRepository extends JpaRepository<DnaGlossary, String> {
    Optional<DnaGlossary> findByTermAndDomainId(String term, String domainId);
    Optional<DnaGlossary> findByTerm(String term);
    List<DnaGlossary> findByDomainId(String domainId);
    
    @Query("SELECT g FROM DnaGlossary g WHERE g.status = 'active' AND (g.domainId = :domainId OR g.domainId IS NULL) ORDER BY g.domainId ASC, g.term ASC")
    List<DnaGlossary> findActiveByScope(String domainId);
    
    long countByDomainIdAndStatus(String domainId, String status);
}
