package com.summa.repository;

import com.summa.model.SpendLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;

@Repository
public interface SpendLedgerRepository extends JpaRepository<SpendLedger, String> {
    @Query("SELECT COALESCE(SUM(s.cost), 0.0) FROM SpendLedger s WHERE s.kind = 'settle' AND s.at >= :since")
    double sumSettleCostSince(@org.springframework.data.repository.query.Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(s.cost), 0.0) FROM SpendLedger s WHERE s.kind = 'reserve' AND s.at >= :since")
    double sumReservedSince(@org.springframework.data.repository.query.Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(s.cost), 0.0) FROM SpendLedger s WHERE s.kind = 'settle' AND s.at >= :since AND s.acknowledged = false")
    double sumUnacknowledgedSettleCostSince(@org.springframework.data.repository.query.Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(s.cost), 0.0) FROM SpendLedger s WHERE s.kind = 'reserve' AND s.at >= :since AND s.acknowledged = false")
    double sumUnacknowledgedReservedSince(@org.springframework.data.repository.query.Param("since") Instant since);
}
