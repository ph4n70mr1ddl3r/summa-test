package com.summa.service;

import com.summa.repository.DnaDomainRepository;
import com.summa.repository.DnaProposalRepository;
import com.summa.model.DnaDomain;
import com.summa.model.DnaProposal;
import com.summa.model.Agent;
import com.summa.model.Human;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DnaReadService {
    private final DnaDomainRepository domainRepository;
    private final DnaProposalRepository proposalRepository;
    private final MemberService memberService;
    private final JdbcTemplate jdbcTemplate;

    public DnaReadService(DnaDomainRepository domainRepository,
                          DnaProposalRepository proposalRepository,
                          MemberService memberService,
                          org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.domainRepository = domainRepository;
        this.proposalRepository = proposalRepository;
        this.memberService = memberService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Sanitize FTS5 query input by escaping or rejecting special operators.
     * Prevents injection of FTS5 operators (*, OR, NOT, phrase syntax) via user input.
     */
    private static String sanitizeFtsQuery(String query) {
        // Strip all FTS5 boolean operators, wildcards, and phrase syntax to prevent injection.
        // We tokenize on whitespace after stripping operators so the remaining words form a
        // safe term list — FTS5 concatenates term lists with implicit AND.
        String sanitized = query
            .replace("*", "")
            .replaceAll("(?i)\\bOR\\b", " ")
            .replaceAll("(?i)\\bAND\\b", " ")
            .replaceAll("(?i)\\bNOT\\b", " ")
            .replace("|", " ")
            .replace("(", " ")
            .replace(")", " ")
            .replace("[", " ")
            .replace("]", " ")
            .replace("{", " ")
            .replace("}", " ")
            .replace("\"", " ")
            .replaceAll("[+\\-<>]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        return sanitized;
    }

    /**
     * Search DNA using FTS5 full-text search.
     * Implements DRP-030: search serves living corpus (active items only).
     */
    public List<Map<String, Object>> search(String query, String domainId, int limit) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be blank");
        }
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        // FTS5 matches against the table (not a single column) and user input
        // must be quoted — raw input can inject FTS5 operators (e.g. `"`, `*`, `OR`).
        String safeQuery = sanitizeFtsQuery(query);
        if (safeQuery.isBlank()) {
            throw new IllegalArgumentException("Search query contains only FTS5 operators");
        }
        String match = "\"" + safeQuery + "\"*";
        String sql = "SELECT id, title, definition_md, statement_md, context_md, outcome_md, " +
                     "term, definition, content_md, domain_id, kind, status " +
                     "FROM dna_search_index " +
                     "WHERE dna_search_index MATCH ? AND status = 'active' " +
                     (domainId != null ? "AND domain_id = ? " : "") +
                     "ORDER BY rank LIMIT ?";

        Object[] params;
        if (domainId != null) {
            params = new Object[]{match, domainId, safeLimit};
        } else {
            params = new Object[]{match, safeLimit};
        }

        return jdbcTemplate.query(sql, rs -> {
            List<Map<String, Object>> results = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getString("id"));
                row.put("kind", rs.getString("kind"));
                row.put("domain_id", rs.getString("domain_id"));
                row.put("title", rs.getString("title"));
                row.put("definition", rs.getString("definition_md") != null ? rs.getString("definition_md") : rs.getString("definition"));
                results.add(row);
            }
            return results;
        }, params);
    }

    /**
     * Get all active domains with their reader sets.
     */
    public List<DnaDomain> listDomains() {
        return domainRepository.findAllActive();
    }

    /**
     * Get open proposals for a domain (review queue).
     */
    public List<DnaProposal> getReviewQueue(String domainId) {
        if (domainId != null) {
            return proposalRepository.findOpenByDomain(domainId);
        }
        return proposalRepository.findAllOpen();
    }

    /**
     * Get org snapshot for prompt injection (DRP-001, DRP-002).
     * Returns live members with their states.
     */
    public Map<String, Object> getOrgSnapshot(String actorId) {
        List<Human> activeHumans = memberService.findAllActiveHumans();
        List<Agent> activeAgents = memberService.findAllActiveAgents();
        List<DnaDomain> domains = domainRepository.findAllActive();
        
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("humans", activeHumans.stream().map(h -> Map.of(
            "id", h.getId(),
            "name", h.getName(),
            "rbac", h.getRbac()
        )).toList());
        snapshot.put("agents", activeAgents.stream().map(a -> Map.of(
            "id", a.getId(),
            "name", a.getName(),
            "class", a.getAgentClass(),
            "status", a.getStatus(),
            "suspendedAt", a.getSuspendedAt() != null ? a.getSuspendedAt().toString() : null,
            "retiredAt", a.getRetiredAt() != null ? a.getRetiredAt().toString() : null
        )).toList());
        snapshot.put("domains", domains.stream().map(d -> Map.of(
            "id", d.getId(),
            "name", d.getName(),
            "access", d.getAccess()
        )).toList());
        
        return snapshot;
    }
}
