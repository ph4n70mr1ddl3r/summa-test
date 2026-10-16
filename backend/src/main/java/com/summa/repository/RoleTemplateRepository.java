package com.summa.repository;

import com.summa.model.RoleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleTemplateRepository extends JpaRepository<RoleTemplate, String> {
    Optional<RoleTemplate> findByName(String name);
}
