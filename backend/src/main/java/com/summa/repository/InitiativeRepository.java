package com.summa.repository;

import com.summa.model.Initiative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InitiativeRepository extends JpaRepository<Initiative, String> {
    List<Initiative> findByStatus(String status);
    
    List<Initiative> findBySponsor(String sponsor);
    
    List<Initiative> findByLead(String lead);
}
