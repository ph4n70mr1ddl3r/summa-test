package com.summa.repository;

import com.summa.model.Ask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AskRepository extends JpaRepository<Ask, String> {
    List<Ask> findByStatus(String status);

    List<Ask> findByTo(String to);

    @Query("SELECT a FROM Ask a WHERE a.to = :to AND a.status = 'pending' ORDER BY a.deadline ASC")
    List<Ask> findByToAndStatusPending(String to);

    @Query("SELECT a FROM Ask a WHERE a.to = :to AND a.status = 'pending' ORDER BY a.deadline ASC LIMIT :limit")
    List<Ask> findByToAndStatusPending(@org.springframework.data.repository.query.Param("to") String to,
                                       @org.springframework.data.repository.query.Param("limit") int limit);

    long countByStatus(String status);

    @Query("SELECT a FROM Ask a WHERE a.status = 'pending' AND a.deadline < :now")
    List<Ask> findExpiredBefore(java.time.Instant now);

    @Query("SELECT a FROM Ask a WHERE a.from = :agentId AND a.status = 'pending'")
    List<Ask> findByFromAndStatusPending(String agentId);

    @Query("SELECT a FROM Ask a WHERE a.initiativeId = :initiativeId AND a.status = 'pending'")
    List<Ask> findByInitiativeIdAndStatusPending(String initiativeId);

    @Query("SELECT a FROM Ask a WHERE a.workspaceId = :workspaceId AND a.status = 'pending'")
    List<Ask> findByWorkspaceIdAndStatusPending(String workspaceId);

    @Query("SELECT a FROM Ask a WHERE a.status = :status ORDER BY a.deadline ASC LIMIT :limit")
    List<Ask> findByStatusOrdered(@org.springframework.data.repository.query.Param("status") String status,
                                  @org.springframework.data.repository.query.Param("limit") int limit);

    @Query("SELECT a FROM Ask a WHERE a.status = 'pending' ORDER BY a.deadline ASC LIMIT :limit")
    List<Ask> findByStatusPendingOrdered(@org.springframework.data.repository.query.Param("limit") int limit);
}
