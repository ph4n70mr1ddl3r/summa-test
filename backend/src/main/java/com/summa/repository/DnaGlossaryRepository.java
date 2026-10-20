package com.summa.repository;

import com.summa.model.DnaGlossary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DnaGlossaryRepository extends JpaRepository<DnaGlossary, String> {
    Optional<DnaGlossary> findByTermAndDomainId(String term, String domainId);
    List<DnaGlossary> findByDomainId(String domainId);

    /**
     * DNC-041/042: the "live entry" of the duplicate check is any non-retired row of
     * the same scope — draft and active both hold their terms, and retirement is what
     * frees a term for reuse. The scope comparison treats the org-wide (null domain)
     * scope as a scope of its own, so an org-wide entry is checked against the other
     * org-wide entries.
     *
     * <p>The plain {@code findByTermAndDomainId} cannot answer this: the partial
     * unique index covers non-retired rows only, so a scope legitimately holds one
     * retired and one live row — and the derived single-result query blew up with
     * an incorrect-result-size error instead of refusing the write.
     */
    @Query(value = "SELECT g.* FROM dna_glossary g WHERE g.term = :term AND g.status <> 'retired' "
            + "AND COALESCE(g.domain_id, '') = COALESCE(:domainId, '')", nativeQuery = true)
    List<DnaGlossary> findLiveByTermAndScope(@org.springframework.data.repository.query.Param("term") String term,
                                              @org.springframework.data.repository.query.Param("domainId") String domainId);

    @Query("SELECT g FROM DnaGlossary g WHERE g.status = 'active' AND (g.domainId = :domainId OR g.domainId IS NULL) ORDER BY g.domainId ASC, g.term ASC")
    List<DnaGlossary> findActiveByScope(@org.springframework.data.repository.query.Param("domainId") String domainId);

    List<DnaGlossary> findByStatus(String status);
    long countByDomainIdAndStatus(String domainId, String status);
}
