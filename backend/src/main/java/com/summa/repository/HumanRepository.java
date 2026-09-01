package com.summa.repository;

import com.summa.model.Human;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface HumanRepository extends JpaRepository<Human, String> {
    Optional<Human> findByEmail(String email);
    Optional<Human> findByName(String name);
    
    @Query("SELECT h FROM Human h WHERE h.deactivatedAt IS NULL AND h.rbac = :role")
    java.util.List<Human> findActiveByRole(String role);
    
    @Query("SELECT h FROM Human h WHERE h.deactivatedAt IS NULL")
    java.util.List<Human> findAllActive();
    
    long countByDeactivatedAtIsNull();
    
    long countByDeactivatedAtIsNullAndRbac(String rbac);

    /**
     * OFB-020: Pessimistic-write lock on the target human row so that a concurrent
     * offboard of a different admin cannot slip between our admin-count check and
     * our deactivate, leaving zero live admins.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Human h WHERE h.id = :id")
    Optional<Human> findByIdForUpdate(String id);
}
