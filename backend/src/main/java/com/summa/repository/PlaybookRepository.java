package com.summa.repository;

import com.summa.model.Playbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlaybookRepository extends JpaRepository<Playbook, String> {
    List<Playbook> findByStatus(String status);
    List<Playbook> findByCreatedBy(String createdBy);
}
