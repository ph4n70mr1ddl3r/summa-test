package com.summa.repository;

import com.summa.model.RoleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleTemplateRepository extends JpaRepository<RoleTemplate, String> {
    Optional<RoleTemplate> findByName(String name);
    
    @Query("SELECT t FROM RoleTemplate t WHERE t.agentClass = :clazz AND t.status = 'active' ORDER BY t.version DESC")
    List<RoleTemplate> findActiveByClass(@org.springframework.data.repository.query.Param("clazz") String clazz);

    @Query("SELECT t FROM RoleTemplate t WHERE t.name = :name AND t.status = 'active' ORDER BY t.version DESC")
    List<RoleTemplate> findActiveByName(@org.springframework.data.repository.query.Param("name") String name);

    @Query("SELECT t FROM RoleTemplate t WHERE t.agentClass = :clazz AND t.name = :name ORDER BY t.version DESC")
    List<RoleTemplate> findByClassAndName(@org.springframework.data.repository.query.Param("clazz") String clazz,
                                            @org.springframework.data.repository.query.Param("name") String name);
}
