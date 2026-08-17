package com.summa.repository;

import com.summa.model.ExternalWrite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface ExternalWriteRepository extends JpaRepository<ExternalWrite, String> {
    List<ExternalWrite> findByStatus(String status);
    List<ExternalWrite> findByRunId(String runId);
    
    @Query("SELECT e FROM ExternalWrite e WHERE e.status = 'prepared' AND e.preparedAt < :before")
    List<ExternalWrite> findStrandedBefore(Instant before);
}
