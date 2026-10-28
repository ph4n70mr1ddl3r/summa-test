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

    @Query("SELECT COALESCE(SUM(CASE WHEN s.kind = 'reserve' THEN s.cost WHEN s.kind = 'release' THEN -s.cost ELSE 0.0 END), 0.0) FROM SpendLedger s WHERE s.kind IN ('reserve', 'release') AND s.at >= :since")
    double sumReservedSince(@org.springframework.data.repository.query.Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(s.cost), 0.0) FROM SpendLedger s WHERE s.kind = 'settle' AND s.at >= :since AND s.acknowledged = false")
    double sumUnacknowledgedSettleCostSince(@org.springframework.data.repository.query.Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(CASE WHEN s.kind = 'reserve' THEN s.cost WHEN s.kind = 'release' THEN -s.cost ELSE 0.0 END), 0.0) FROM SpendLedger s WHERE s.kind IN ('reserve', 'release') AND s.at >= :since AND s.acknowledged = false")
    double sumUnacknowledgedReservedSince(@org.springframework.data.repository.query.Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(CASE WHEN s.kind = 'reserve' THEN s.cost WHEN s.kind = 'release' THEN -s.cost ELSE 0.0 END), 0.0) FROM SpendLedger s WHERE s.runId = :runId AND s.kind IN ('reserve', 'release')")
    double sumNetReserveForRun(@org.springframework.data.repository.query.Param("runId") String runId);

    @Query("SELECT COALESCE(SUM(CASE WHEN s.kind = 'reserve' THEN s.cost WHEN s.kind = 'release' THEN -s.cost ELSE 0.0 END), 0.0) FROM SpendLedger s WHERE s.spawnId = :spawnId AND s.kind IN ('reserve', 'release')")
    double sumNetReserveForSpawn(@org.springframework.data.repository.query.Param("spawnId") String spawnId);

    @Query("SELECT COUNT(s) FROM SpendLedger s WHERE s.kind = 'settle' AND s.acknowledged = false")
    long countUnacknowledgedOverruns();
}
