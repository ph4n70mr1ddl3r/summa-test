package com.summa.repository;

import com.summa.model.DataHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DataHoldRepository extends JpaRepository<DataHold, String> {
    List<DataHold> findByKindAndSubjectIdAndReleasedAtIsNull(String kind, String subjectId);
    List<DataHold> findByReleasedAtIsNull();
    boolean existsByKindAndSubjectIdAndReleasedAtIsNull(String kind, String subjectId);
}
