package com.summa.repository;

import com.summa.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, String> {
    Optional<Group> findByName(String name);

    Optional<Group> findByNameAndStatusNot(String name, String status);

    @Query("SELECT g FROM Group g LIMIT :limit")
    List<Group> findAll(@org.springframework.data.repository.query.Param("limit") int limit);
}
