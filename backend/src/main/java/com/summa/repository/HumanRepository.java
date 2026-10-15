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
    
    @Query("SELECT h FROM Human h WHERE h.deactivatedAt IS NULL AND h.rbac = :role")
    java.util.List<Human> findActiveByRole(@org.springframework.data.repository.query.Param("role") String role);
    
    @Query("SELECT h FROM Human h WHERE h.deactivatedAt IS NULL")
    java.util.List<Human> findAllActive();

    long countByDeactivatedAtIsNullAndRbac(String rbac);

    /**
     * OFB-020: Read the target human row under a pessimistic lock when the database
     * supports it (PostgreSQL, H2). Falls back to a plain select on SQLite, which
     * does not support SELECT ... FOR UPDATE. The admin-count guard below is still
     * safe because the transaction boundary and single-writer model make the race
     * window negligible; the lock is best-effort.
     * The @Param name is the parameter name required by @Query; Spring Data does
     * not apply parameter-name discovery to @Query methods.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Human h WHERE h.id = :id")
    Optional<Human> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") String id);
}
