package com.summa.repository;

import com.summa.model.DnaProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DnaProposalRepository extends JpaRepository<DnaProposal, String> {
    List<DnaProposal> findByStatus(String status);
    
    List<DnaProposal> findByDomainId(String domainId);
    
    @Query("SELECT p FROM DnaProposal p WHERE p.domainId = :domainId AND p.status = 'open' ORDER BY p.createdAt ASC")
    List<DnaProposal> findOpenByDomain(String domainId);
    
    @Query("SELECT p FROM DnaProposal p WHERE p.status = 'open' ORDER BY p.reviewBy ASC, p.createdAt ASC")
    List<DnaProposal> findAllOpen();
    
    long countByStatus(String status);
    long countByDomainIdAndStatus(String domainId, String status);
}
