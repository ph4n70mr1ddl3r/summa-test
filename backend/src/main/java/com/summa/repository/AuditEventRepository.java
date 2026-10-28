package com.summa.repository;

import com.summa.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {
    @Query("SELECT a FROM AuditEvent a WHERE a.objectType = :type AND a.objectId = :id ORDER BY a.at DESC")
    List<AuditEvent> findByObject(@org.springframework.data.repository.query.Param("type") String type,
                                   @org.springframework.data.repository.query.Param("id") String id);

    @Query("SELECT a FROM AuditEvent a WHERE a.actor = :actor ORDER BY a.at DESC")
    List<AuditEvent> findByActor(@org.springframework.data.repository.query.Param("actor") String actor);
    
    @Query("SELECT a FROM AuditEvent a ORDER BY a.at DESC LIMIT :limit")
    List<AuditEvent> findRecent(@org.springframework.data.repository.query.Param("limit") int limit);
    
    long countByAtAfter(java.time.Instant since);

    /**
     * STG-030/DAT-121: erasure rewrites the actor reference to its one-way
     * pseudonym without deleting the row. This is the single narrow exception to
     * the append-only rule, so it is a bulk update that deliberately bypasses
     * {@link AuditEvent#preUpdate()} (which refuses ordinary updates).
     */
    @Modifying
    @Query("UPDATE AuditEvent a SET a.actor = :pseudonym WHERE a.actor IN :actors")
    int pseudonymizeActor(@org.springframework.data.repository.query.Param("actors") List<String> actors,
                          @org.springframework.data.repository.query.Param("pseudonym") String pseudonym);
}
