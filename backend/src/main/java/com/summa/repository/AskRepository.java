package com.summa.repository;

import com.summa.model.Ask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AskRepository extends JpaRepository<Ask, String> {
    List<Ask> findByStatus(String status);

    @Query("SELECT a FROM Ask a WHERE a.to = :to AND a.status = 'pending' ORDER BY a.deadline ASC")
    List<Ask> findByToAndStatusPending(@org.springframework.data.repository.query.Param("to") String to);

    @Query("SELECT a FROM Ask a WHERE a.to = :to AND a.status = 'pending' ORDER BY a.deadline ASC LIMIT :limit")
    List<Ask> findByToAndStatusPending(@org.springframework.data.repository.query.Param("to") String to,
                                       @org.springframework.data.repository.query.Param("limit") int limit);

    long countByStatus(String status);

    @Query("SELECT a FROM Ask a WHERE a.status = 'pending' AND a.deadline < :now")
    List<Ask> findExpiredBefore(@org.springframework.data.repository.query.Param("now") java.time.Instant now);

    @Query("SELECT a FROM Ask a WHERE a.from = :agentId AND a.status = 'pending'")
    List<Ask> findByFromAndStatusPending(@org.springframework.data.repository.query.Param("agentId") String agentId);

    @Query("SELECT a FROM Ask a WHERE a.initiativeId = :initiativeId AND a.status = 'pending'")
    List<Ask> findByInitiativeIdAndStatusPending(@org.springframework.data.repository.query.Param("initiativeId") String initiativeId);

    @Query("SELECT a FROM Ask a WHERE a.workspaceId = :workspaceId AND a.status = 'pending'")
    List<Ask> findByWorkspaceIdAndStatusPending(@org.springframework.data.repository.query.Param("workspaceId") String workspaceId);

    @Query("SELECT a FROM Ask a WHERE a.status = :status ORDER BY a.deadline ASC LIMIT :limit")
    List<Ask> findByStatusOrdered(@org.springframework.data.repository.query.Param("status") String status,
                                  @org.springframework.data.repository.query.Param("limit") int limit);

    @Query("SELECT a FROM Ask a WHERE a.to = :to AND a.status = :status ORDER BY a.deadline ASC LIMIT :limit")
    List<Ask> findByToAndStatusOrdered(@org.springframework.data.repository.query.Param("to") String to,
                                       @org.springframework.data.repository.query.Param("status") String status,
                                       @org.springframework.data.repository.query.Param("limit") int limit);

    @Query("SELECT a FROM Ask a WHERE a.status = 'pending' ORDER BY a.deadline ASC LIMIT :limit")
    List<Ask> findByStatusPendingOrdered(@org.springframework.data.repository.query.Param("limit") int limit);

    @Query("SELECT a FROM Ask a WHERE a.from = :from")
    List<Ask> findByFrom(@org.springframework.data.repository.query.Param("from") String from);

    @Query("SELECT a FROM Ask a WHERE a.to = :to")
    List<Ask> findByTo(@org.springframework.data.repository.query.Param("to") String to);

    @Query("SELECT a FROM Ask a WHERE a.initiativeId = :initiativeId AND a.kind = 'approval' AND a.status = 'pending'")
    List<Ask> findByInitiativeIdAndKindApprovalPending(@org.springframework.data.repository.query.Param("initiativeId") String initiativeId);

    /**
     * STG-031: the erasure sweep's ledger lookup — asks whose responses ledger
     * names the member as a responder. The needle is a JSON substring (the
     * stored ledger is a serialized array of responder entries), so the caller
     * supplies both the bare and the h:-prefixed form; the sweep re-checks each
     * entry exactly before rewriting.
     */
    @Query("SELECT a FROM Ask a WHERE a.responses LIKE :needle")
    List<Ask> findByResponsesContaining(@org.springframework.data.repository.query.Param("needle") String needle);
}
