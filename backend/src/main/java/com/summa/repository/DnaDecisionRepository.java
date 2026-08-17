package com.summa.repository;

import com.summa.model.DnaDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DnaDecisionRepository extends JpaRepository<DnaDecision, String> {
    List<DnaDecision> findByDomainId(String domainId);
}
