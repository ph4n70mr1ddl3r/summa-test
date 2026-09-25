package com.summa.repository;

import com.summa.model.Initiative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InitiativeRepository extends JpaRepository<Initiative, String> {
    List<Initiative> findByStatus(String status);

    List<Initiative> findBySponsor(String sponsor);

    List<Initiative> findByLead(String lead);

    List<Initiative> findAll();

    /**
     * Find non-closed initiatives whose depends_on JSON array contains the given id.
     * Avoids a full-table scan followed in-memory filtering.
     * Escapes SQL LIKE wildcards in initId to prevent pattern injection.
     */
    @Query("SELECT i FROM Initiative i WHERE i.status != 'closed' AND i.dependsOn IS NOT NULL AND i.dependsOn LIKE %:escapedId% ESCAPE '/'")
    List<Initiative> findByDependsOnContaining(@Param("escapedId") String initId);

    /**
     * Escape SQL LIKE wildcards (% and _) so the query treats them as literals.
     * Initiative IDs are UUIDs but the guard prevents future format changes from breaking the query.
     */
    static String escapeLike(String value) {
        if (value == null) return null;
        return value.replace("/", "//").replace("%", "/%").replace("_", "/_");
    }
}
