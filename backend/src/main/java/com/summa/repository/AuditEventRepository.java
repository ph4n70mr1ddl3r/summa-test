package com.summa.repository;

import com.summa.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {
    @Query("SELECT a FROM AuditEvent a WHERE a.objectType = :type AND a.objectId = :id ORDER BY a.at DESC")
    List<AuditEvent> findByObject(String type, String id);
    
    @Query("SELECT a FROM AuditEvent a WHERE a.actor = :actor ORDER BY a.at DESC")
    List<AuditEvent> findByActor(String actor);
    
    @Query("SELECT a FROM AuditEvent a ORDER BY a.at DESC")
    List<AuditEvent> findRecent(int limit);
    
    long countByAtAfter(java.time.Instant since);
}
