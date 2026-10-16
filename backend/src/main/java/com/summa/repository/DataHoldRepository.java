package com.summa.repository;

import com.summa.model.DataHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DataHoldRepository extends JpaRepository<DataHold, String> {
    List<DataHold> findByKindAndSubjectIdAndReleasedAtIsNull(String kind, String subjectId);
    List<DataHold> findByReleasedAtIsNull();

    @Query("SELECT h FROM DataHold h WHERE h.releasedAt IS NULL ORDER BY h.createdAt DESC LIMIT :limit")
    List<DataHold> findReleasedOrderByCreatedAtDesc(@Param("limit") int limit);
    boolean existsByKindAndSubjectIdAndReleasedAtIsNull(String kind, String subjectId);
}
