package com.summa.repository;

import com.summa.model.SpendLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface SpendLedgerRepository extends JpaRepository<SpendLedger, String> {
    List<SpendLedger> findByMemberId(String memberId);
    
    @Query("SELECT SUM(s.cost) FROM SpendLedger s WHERE s.memberId = :memberId AND s.at >= :since")
    Double sumCostByMemberSince(String memberId, Instant since);
    
    @Query("SELECT SUM(s.cost) FROM SpendLedger s WHERE s.at >= :since")
    Double sumTotalCostSince(Instant since);
    
    List<SpendLedger> findByKind(String kind);
}
