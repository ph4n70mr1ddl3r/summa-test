package com.summa.service;

import com.summa.repository.DnaRuleRepository;
import com.summa.model.DnaRule;
import com.summa.model.DnaDomain;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.summa.util.ScanUtils;
import com.summa.util.JsonHelpers;
import com.summa.util.DomainGuards;
import com.summa.exception.EntityNotFoundException;

@Service
public class DnaRuleService {
    private final DnaRuleRepository ruleRepository;
    private final AuditService auditService;
    private final DnaDomainService domainService;
    private final SecretsScanner secretsScanner;
    private final DomainLockService domainLockService;

    public DnaRuleService(DnaRuleRepository ruleRepository, AuditService auditService,
                          DnaDomainService domainService, SecretsScanner secretsScanner) {
        this(ruleRepository, auditService, domainService, secretsScanner, new DomainLockService());
    }

    public DnaRuleService(DnaRuleRepository ruleRepository, AuditService auditService,
                          DnaDomainService domainService, SecretsScanner secretsScanner,
                          DomainLockService domainLockService) {
        this.ruleRepository = ruleRepository;
        this.auditService = auditService;
        this.domainService = domainService;
        this.secretsScanner = secretsScanner;
        this.domainLockService = domainLockService != null ? domainLockService : new DomainLockService();
    }

    @Transactional
    public DnaRule create(String id, String domainId, String statementMd, String machineHint,
                          Instant effectiveFrom, Instant effectiveTo, String supersedesId,
                          String actor) {
        return domainLockService.withLock(domainId, () -> {
            // DGV-041: an archived domain is read-only history — no new items land in it.
            requireWritableDomain(domainId);
            ScanUtils.scanForSecrets(statementMd, actor, "dna_rule", id, secretsScanner, auditService);
            // Validate effective date ordering
            if (effectiveTo != null && effectiveFrom != null && effectiveTo.isBefore(effectiveFrom)) {
                throw new IllegalArgumentException("effectiveTo must not be before effectiveFrom");
            }

            // Validate supersedes_id is in same domain
            DnaRule predecessor = null;
            if (supersedesId != null) {
                Optional<DnaRule> existingOpt = ruleRepository.findById(supersedesId);
                if (existingOpt.isEmpty()) {
                    throw new IllegalArgumentException("Supersedes rule not found: " + supersedesId);
                }
                DnaRule existing = existingOpt.get();
                if (!Objects.equals(existing.getDomainId(), domainId)) {
                    throw new IllegalArgumentException("Supersedes rule must be in the same domain");
                }
                if (!"active".equals(existing.getStatus()) && !"superseded".equals(existing.getStatus())) {
                    throw new IllegalArgumentException("Cannot supersede a lapsed rule");
                }

                // Check for forked supersession
                long successorCount = ruleRepository.findBySupersedesId(supersedesId).size();
                if (successorCount > 0) {
                    throw new IllegalArgumentException(
                        "Rule already has a superseder. Name the chain's live head instead.");
                }
                predecessor = existing;
            }

            DnaRule rule = new DnaRule();
            rule.setId(id);
            rule.setDomainId(domainId);
            rule.setStatementMd(statementMd != null ? statementMd : "");
            rule.setMachineHint(machineHint != null ? machineHint : "");
            rule.setEffectiveFrom(effectiveFrom);
            rule.setEffectiveTo(effectiveTo);
            rule.setSupersedesId(supersedesId);
            if (effectiveTo != null && effectiveTo.isBefore(Instant.now())) {
                rule.setStatus("lapsed");
            } else {
                rule.setStatus("active");
            }

            DnaRule saved = ruleRepository.save(rule);
            // DNC-023: naming a predecessor displaces it at the successor's window
            // edge — the create door is as much a displacement door as the explicit
            // supersede door, so a chain is never left with two co-temporal heads.
            if (predecessor != null) {
                displacePredecessor(predecessor, saved.getEffectiveFrom(), actor);
            }
            auditService.log(actor, "CREATE_RULE", "dna_rule", id,
                String.format("{\"domainId\":%s,\"effectiveFrom\":%s}", JsonHelpers.jsonString(domainId), JsonHelpers.jsonString(effectiveFrom != null ? String.valueOf(effectiveFrom) : null)));
            return saved;
        });
    }

    /**
     * DGV-041: the archived domain row is read-only history — no new items, no
     * writes into frozen history. A rule write names its domain, so the door
     * resolves the domain and refuses an archived one.
     */
    private void requireWritableDomain(String domainId) {
        if (domainId == null || domainId.isBlank()) return;
        Optional<DnaDomain> domainOpt = domainService.findById(domainId);
        DomainGuards.requireWritable(domainOpt.orElse(null), domainId);
    }

    /**
     * DNC-023: "The displacement edge is the superseder's `effective_from`: a
     * predecessor keeps injecting until its superseder's window opens; a
     * future-windowed successor is a scheduled replacement, never a normative
     * gap."
     *
     * <p>Two moves make that true:
     * <ul>
     *   <li>the predecessor's injection window is truncated at the successor's
     *       {@code effective_from} — the edge — so before the edge the predecessor
     *       still injects and the successor does not, and from the edge the
     *       predecessor does not and the successor does. There is no gap and no
     *       co-temporal pair, whatever the sweep's cadence;</li>
     *   <li>the status transition to {@code superseded} is applied immediately
     *       when the edge has already passed, and otherwise left to the expiry
     *       sweep, which applies it the moment the edge arrives — a predecessor
     *       still inside its own window is not yet displaced (DNC-022).</li>
     * </ul>
     *
     * <p>The window is only ever shortened: a predecessor whose own window ends
     * earlier keeps that earlier end.
     */
    private void displacePredecessor(DnaRule predecessor, Instant successorEffectiveFrom, String actor) {
        if (successorEffectiveFrom == null) return;
        Instant currentEnd = predecessor.getEffectiveTo();
        if (currentEnd == null || successorEffectiveFrom.isBefore(currentEnd)) {
            predecessor.setEffectiveTo(successorEffectiveFrom);
        }
        if (!successorEffectiveFrom.isAfter(Instant.now())) {
            predecessor.setStatus("superseded");
        }
        ruleRepository.save(predecessor);
        auditService.log(actor, "DISPLACE_RULE", "dna_rule", predecessor.getId(),
            String.format("{\"successorEffectiveFrom\":%s,\"status\":%s}",
                JsonHelpers.jsonString(String.valueOf(successorEffectiveFrom)),
                JsonHelpers.jsonString(predecessor.getStatus())));
    }

    @Transactional(readOnly = true)
    public Optional<DnaRule> findById(String id) {
        return ruleRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<DnaRule> findByDomain(String domainId) {
        return ruleRepository.findByDomainId(domainId);
    }

    @Transactional(readOnly = true)
    public List<DnaRule> findActiveWindowed(String domainId, Instant now) {
        return ruleRepository.findActiveWindowed(domainId, now);
    }

    @Transactional(readOnly = true)
    public List<DnaRule> findAllActiveWindowed(Instant now) {
        return ruleRepository.findAllActiveWindowed(now);
    }

    @Transactional
    public DnaRule update(String id, String statementMd, String machineHint,
                          Instant effectiveTo, String actor) {
        DnaRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found: " + id));

        return domainLockService.withLock(rule.getDomainId(), () -> {
            if (!"active".equals(rule.getStatus())) {
                throw new IllegalArgumentException("Cannot update non-active rule: " + rule.getStatus());
            }
            // DGV-041: a rule in an archived domain is frozen history.
            requireWritableDomain(rule.getDomainId());

            // DWP-060: an item-level write gets the same secrets scan a reviewed
            // proposal's publish gets — the update door is not a softer door.
            if (statementMd != null) {
                ScanUtils.scanForSecrets(statementMd, actor, "dna_rule", id, secretsScanner, auditService);
                rule.setStatementMd(statementMd);
            }
            if (machineHint != null) rule.setMachineHint(machineHint);
            if (effectiveTo != null) {
                // DWP-062/DWP-063: one validation at every door — window ordering is
                // checked here exactly as it is at the create door.
                if (rule.getEffectiveFrom() != null && effectiveTo.isBefore(rule.getEffectiveFrom())) {
                    throw new IllegalArgumentException("effectiveTo must not be before effectiveFrom");
                }
                rule.setEffectiveTo(effectiveTo);
                if (effectiveTo.isBefore(Instant.now())) {
                    rule.setStatus("lapsed");
                }
            }

            DnaRule saved = ruleRepository.save(rule);
            auditService.log(actor, "UPDATE_RULE", "dna_rule", id, null);
            return saved;
        });
    }

    /**
     * DNC-021: "Item-level retire on a rule is window truncation: `effective_to`
     * pinned to now, the row lapsing at that boundary." The item surface is
     * create / update / retire, never delete (DWP-061), and a retired rule is
     * frozen history (DNC-022) — the retire is terminal.
     */
    @Transactional
    public DnaRule retire(String id, String actor) {
        DnaRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found: " + id));

        return domainLockService.withLock(rule.getDomainId(), () -> {
            if (!"active".equals(rule.getStatus())) {
                throw new IllegalArgumentException("Cannot retire non-active rule: " + rule.getStatus());
            }
            // DGV-041: a rule in an archived domain is frozen history.
            requireWritableDomain(rule.getDomainId());

            Instant now = Instant.now();
            rule.setEffectiveTo(now);
            rule.setStatus("lapsed");
            DnaRule saved = ruleRepository.save(rule);
            auditService.log(actor, "RETIRE_RULE", "dna_rule", id, null);
            return saved;
        });
    }

    /**
     * DNC-026: "Ordinary expiring rules transition `lapsed` at `effective_to`,
     * dropping out of injection and routing." A rule that was displaced by a
     * successor becomes `superseded` instead — DNC-022: a predecessor stays
     * superseded when its superseder lapses, nothing flips back silently.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void lapseExpiredRules() {
        Instant now = Instant.now();
        for (DnaRule rule : ruleRepository.findByStatus("active")) {
            if (rule.getEffectiveTo() == null || rule.getEffectiveTo().isAfter(now)) continue;
            boolean displaced = ruleRepository.findBySupersedesId(rule.getId()).stream()
                    .anyMatch(s -> s.getEffectiveFrom() == null || !s.getEffectiveFrom().isAfter(now));
            rule.setStatus(displaced ? "superseded" : "lapsed");
            ruleRepository.save(rule);
            auditService.logSystem(displaced ? "SUPERSEDE_RULE" : "LAPSE_RULE", "dna_rule", rule.getId(),
                String.format("{\"effectiveTo\":%s,\"status\":%s}",
                    JsonHelpers.jsonString(String.valueOf(rule.getEffectiveTo())),
                    JsonHelpers.jsonString(rule.getStatus())));
        }
    }

    @Transactional
    public DnaRule supersede(String id, String supersedesId, String actor) {
        DnaRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found: " + id));

        return domainLockService.withLock(rule.getDomainId(), () -> {
            DnaRule predecessor = ruleRepository.findById(supersedesId)
                    .orElseThrow(() -> new EntityNotFoundException("Predecessor rule not found: " + supersedesId));

            if (!predecessor.getDomainId().equals(rule.getDomainId())) {
                throw new IllegalArgumentException("Cross-domain supersession not allowed");
            }
            // DGV-041: a rule in an archived domain is frozen history.
            requireWritableDomain(rule.getDomainId());

            // DNC-025: supersession chains are linear, not forks — a second live
            // `supersedes_id` edge onto an already-superseded row is refused at
            // propose, amend, and item write; displacing a superseded rule means
            // naming the chain's live head. The create door carries these guards; the
            // item-write door must carry them too or a fork is reachable from the API.
            long successorCount = ruleRepository.findBySupersedesId(supersedesId).size();
            if (successorCount > 0) {
                throw new IllegalArgumentException(
                    "Rule already has a superseder. Name the chain's live head instead.");
            }
            // DNC-022: superseded and lapsed rules are frozen history — a write must
            // not flip a lapsed rule out of it.
            if (!"active".equals(predecessor.getStatus()) && !"superseded".equals(predecessor.getStatus())) {
                throw new IllegalArgumentException("Cannot supersede a lapsed rule");
            }

            rule.setSupersedesId(supersedesId);

            // Save successor first, then predecessor, within the same transaction
            // If predecessor save fails, the transaction rolls back entirely
            ruleRepository.save(rule);
            // DNC-023: the displacement edge is the successor's `effective_from` — a
            // future-windowed successor is a scheduled replacement, not a gap.
            displacePredecessor(predecessor, rule.getEffectiveFrom(), actor);
            DnaRule saved = ruleRepository.save(predecessor);
            auditService.log(actor, "SUPERSEDE_RULE", "dna_rule", id,
                String.format("{\"supersedes\":%s}", JsonHelpers.jsonString(supersedesId)));
            return saved;
        });
    }
}
