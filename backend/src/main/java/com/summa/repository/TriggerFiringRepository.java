package com.summa.repository;

import com.summa.model.TriggerFiring;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TriggerFiringRepository extends JpaRepository<TriggerFiring, String> {
    Optional<TriggerFiring> findByTriggerIdAndIdempotencyKey(String triggerId, String idempotencyKey);
}
