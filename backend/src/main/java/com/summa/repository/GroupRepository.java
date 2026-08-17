package com.summa.repository;

import com.summa.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, String> {
    Optional<Group> findByName(String name);
    
    Optional<Group> findByNameAndStatusNot(String name, String status);
}
