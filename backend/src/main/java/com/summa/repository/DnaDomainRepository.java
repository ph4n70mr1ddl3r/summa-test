package com.summa.repository;

import com.summa.model.DnaDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DnaDomainRepository extends JpaRepository<DnaDomain, String> {
    Optional<DnaDomain> findByName(String name);
    
    @Query("SELECT d FROM DnaDomain d WHERE d.status != 'archived' AND d.name = :name")
    Optional<DnaDomain> findByNameNotArchived(String name);
    
    List<DnaDomain> findByStatus(String status);
    
    @Query("SELECT d FROM DnaDomain d WHERE d.status != 'archived'")
    List<DnaDomain> findAllActive();
    
    @Query("SELECT d FROM DnaDomain d WHERE d.ownerHumanId = :ownerId AND d.status != 'archived'")
    List<DnaDomain> findByOwnerHumanId(String ownerId);
}
