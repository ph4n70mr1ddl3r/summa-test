package com.summa.repository;

import com.summa.model.DnaCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DnaCardRepository extends JpaRepository<DnaCard, String> {
    List<DnaCard> findByDomainId(String domainId);
    List<DnaCard> findByDomainIdAndStatus(String domainId, String status);
    @Query("SELECT c FROM DnaCard c WHERE c.domainId = :domainId AND c.status = 'active' ORDER BY c.title ASC")
    List<DnaCard> findActiveByDomain(String domainId);
    long countByDomainIdAndStatus(String domainId, String status);
}
