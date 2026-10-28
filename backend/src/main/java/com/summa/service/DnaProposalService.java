package com.summa.service;

import com.summa.repository.DnaProposalRepository;
import com.summa.repository.DnaRuleRepository;
import com.summa.model.DnaProposal;
import com.summa.model.DnaDomain;
import com.summa.model.DnaRule;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.summa.util.KeyedUnionValidator;
import com.summa.util.JsonHelpers;
import com.summa.util.ScanUtils;
import com.summa.exception.EntityNotFoundException;
import com.summa.model.Human;
import com.summa.model.Agent;
import com.summa.service.GovernanceService;
import com.summa.constants.Defaults;

@Service
public class DnaProposalService {
    private final DnaProposalRepository proposalRepository;
    private final DnaRuleRepository ruleRepository;
    private final AuditService auditService;
    private final DnaDomainService domainService;
    private final MemberService memberService;
    private final AskService askService;
    private final ObjectMapper objectMapper;
    private final GovernanceService governanceService;
    private final SecretsScanner secretsScanner;
    private final DomainLockService domainLockService;
    private final Set<String> alreadyEscalated = ConcurrentHashMap.newKeySet();

    public DnaProposalService(DnaProposalRepository proposalRepository,
                              DnaRuleRepository ruleRepository,
                              AuditService auditService,
                              DnaDomainService domainService,
                              MemberService memberService,
                              AskService askService,
                              GovernanceService governanceService,
                              ObjectMapper objectMapper) {
        this(proposalRepository, ruleRepository, auditService, domainService,
             memberService, askService, governanceService, objectMapper,
             new SecretsScanner(), new DomainLockService());
    }

    public DnaProposalService(DnaProposalRepository proposalRepository,
                              DnaRuleRepository ruleRepository,
                              AuditService auditService,
                              DnaDomainService domainService,
                              MemberService memberService,
                              AskService askService,
                              GovernanceService governanceService,
                              ObjectMapper objectMapper,
                              SecretsScanner secretsScanner,
                              DomainLockService domainLockService) {
        this.proposalRepository = proposalRepository;
        this.ruleRepository = ruleRepository;
        this.auditService = auditService;
        this.domainService = domainService;
        this.memberService = memberService;
        this.askService = askService;
        this.governanceService = governanceService;
        this.objectMapper = objectMapper;
        this.secretsScanner = secretsScanner != null ? secretsScanner : new SecretsScanner();
        this.domainLockService = domainLockService != null ? domainLockService : new DomainLockService();
    }

    @Transactional
    public DnaProposal create(String id, String kind, String payload, String proposedBy,
                                 String provenance, String domainId) {
        // DAT-120: proposed_by is a keyed union (h:<humans.id> / a:<agents.id>) —
        // the kind is resolved by live lookup so an agent proposer is not
        // relabelled as a human one. DWP-010: the proposer must hold a write
        // surface (a human of any role but viewer, or a persistent agent).
        String proposedByKeyed = memberService.toKeyedMemberRef(proposedBy);
        if (proposedByKeyed == null
                || KeyedUnionValidator.RESERVED_IDS.contains(JsonHelpers.stripIdPrefix(proposedByKeyed))) {
            throw new IllegalArgumentException(
                    "proposedBy must reference an existing human or agent: " + proposedBy);
        }
        requireProposeSurface(proposedByKeyed);
        KeyedUnionValidator.validate(proposedByKeyed, "proposedBy");
        // SEC-030: Secrets scan on proposal payload
        ScanUtils.scanForSecrets(payload, proposedBy, "dna_proposal", id, secretsScanner, auditService);
        DnaProposal proposal = new DnaProposal();
        proposal.setId(id);
        proposal.setKind(kind);
        proposal.setPayload(payload);
        proposal.setProposedBy(proposedByKeyed);
        proposal.setProvenance(provenance != null ? provenance : "{}");
        proposal.setDomainId(domainId);

        // DWP-042: Contradiction detection at proposal time
        List<String> contradictions = detectContradictions(proposal);
        if (!contradictions.isEmpty()) {
            throw new IllegalArgumentException(
                "Contradiction detected: " + String.join("; ", contradictions));
        }

        // DWP-020/021/022: Derive review_by from domain owner (or the admin queue for org-wide).
        // review_by is a humans(id) reference — the reserved 'admins' broadcast addressee
        // is legal on asks.to alone (DAT-120) and would abort the insert here.
        String reviewBy = null;
        if (domainId != null && !domainId.isBlank()) {
            Optional<DnaDomain> domainOpt = domainService.findById(domainId);
            if (domainOpt.isPresent()) {
                reviewBy = domainOpt.get().getOwnerHumanId();
            }
        } else {
            // Org-scoped: the queue is the admin queue — point review_by at a live
            // admin, or leave it null when the org has none (no dangling reference).
            reviewBy = memberService.findAdmins().stream()
                    .map(Human::getId)
                    .findFirst()
                    .orElse(null);
        }
        proposal.setReviewBy(reviewBy);

        // reviewedAt is set only when the proposal is actually reviewed (publish/reject);
        // left as null here to indicate "not yet reviewed"
        
        DnaProposal saved = proposalRepository.save(proposal);
        auditService.log(proposedBy, "PROPOSE", "dna_proposal", id,
            String.format("{\"kind\":%s,\"domainId\":%s}", JsonHelpers.jsonString(kind), JsonHelpers.jsonString(domainId)));
        return saved;
    }

    /**
     * DWP-010: a proposer must hold a write surface — a human of any role but viewer,
     * or a persistent agent (an ephemeral worker's learning folds back to its spawner).
     */
    private void requireProposeSurface(String proposedByKeyed) {
        String clean = JsonHelpers.stripIdPrefix(proposedByKeyed);
        Optional<Human> humanOpt = memberService.findHuman(clean);
        if (humanOpt.isPresent()) {
            if (memberService.isViewer(humanOpt.get())) {
                throw new IllegalArgumentException("viewers cannot propose DNA changes");
            }
            return;
        }
        Optional<Agent> agentOpt = memberService.findAgent(clean);
        if (agentOpt.isPresent()) {
            if (agentOpt.get().isEphemeral()) {
                throw new IllegalArgumentException("ephemeral workers cannot propose DNA changes");
            }
            return;
        }
        throw new IllegalArgumentException("proposedBy must reference an existing human or agent: " + clean);
    }

    @Transactional(readOnly = true)
    public Optional<DnaProposal> findById(String id) {
        return proposalRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<DnaProposal> findByStatus(String status) {
        return proposalRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<DnaProposal> findOpenByDomain(String domainId) {
        return proposalRepository.findOpenByDomain(domainId);
    }

    @Transactional(readOnly = true)
    public List<DnaProposal> findAllOpen() {
        return proposalRepository.findAllOpen();
    }

    @Transactional(readOnly = true)
    public List<DnaProposal> findAll() {
        return proposalRepository.findAll();
    }

    /**
     * DWP-040: Publish runs inside the domain write lock and re-runs contradiction checks
     * against current state at commit. The second of two sequenced contradictory publishes
     * is refused back to review, never half-silently merged.
     */
    @Transactional
    public DnaProposal publish(String id, String reviewedBy, String actor) {
        DnaProposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found: " + id));

        return domainLockService.withLock(proposal.getDomainId(), () -> {
            if (!proposal.isOpen()) {
                // DWP-025: a proposal whose SLA was tightened into the past is flagged
                // 'breached' — still open, surfaced as urgent in the review queue. The
                // publish door admits it; the literal 'open' check bricked it.
                throw new IllegalStateException("Proposal is not open: " + proposal.getStatus());
            }

            // DWP-003: the publish door belongs to the domain owner (the proposal's
            // review_by) or an admin — any other member is refused.
            requireReviewAuthority(proposal, actor);

            // SEC-030 / DWP-060: secrets scan at publish
            ScanUtils.scanForSecrets(proposal.getPayload(), actor, "dna_proposal", id, secretsScanner, auditService);

            // DWP-064: Prevent cross-domain topology smuggling — published payload domain must match proposal domain
            if (proposal.getDomainId() != null && !proposal.getDomainId().isBlank()) {
                try {
                    JsonNode payload = objectMapper.readTree(proposal.getPayload());
                    if (payload.has("domain_id") && !payload.get("domain_id").isNull()) {
                        String payloadDomainId = payload.get("domain_id").asText();
                        if (!payloadDomainId.equals(proposal.getDomainId())) {
                            throw new IllegalStateException(
                                "Domain smuggling blocked: payload domain " + payloadDomainId
                                + " differs from proposal domain " + proposal.getDomainId());
                        }
                    }
                } catch (IllegalStateException e) {
                    throw e;
                } catch (Exception e) {
                    // Non-fatal: payload may not be JSON or may lack domain_id
                }
            }

            // DWP-042: Contradiction detection — re-check against current state
            List<String> contradictions = detectContradictions(proposal);
            if (!contradictions.isEmpty()) {
                throw new IllegalStateException(
                    "Contradiction detected at publish: " + String.join("; ", contradictions));
            }

            // DWP-050: Check SoD — if the domain has sod enabled and the proposer is the publisher,
            // the publish routes to the admin broadcast instead of self-approval.
            String actualReviewer = reviewedBy;
            if (proposal.getDomainId() != null && !proposal.getDomainId().isBlank()) {
                Optional<DnaDomain> domainOpt = domainService.findById(proposal.getDomainId());
                if (domainOpt.isPresent() && "reviewer-distinct".equals(domainOpt.get().getSod())) {
                    // DAT-120: proposed_by is a keyed union and the actor is a bare id —
                    // compare the stripped forms so the separation-of-duties guard fires.
                    if (proposedByMatches(proposal, actor)) {
                        // SoD breach: the proposer may not publish their own proposal.
                        // DWP-050 routes the publish to the admin broadcast (ASK-055):
                        // the proposal stays in review and an admin picks it up. The
                        // publish itself is refused — recording an admin who never saw
                        // the proposal as its reviewer and publishing it anyway made the
                        // knob cosmetic.
                        routeSodPublishToAdmin(proposal);
                        throw new IllegalStateException(
                            "Separation of duties: the proposer may not publish this proposal — "
                                + "the publish is routed to the admin broadcast");
                    }
                }
            }

            proposal.setStatus("published");
            proposal.setReviewedBy(actualReviewer);
            proposal.setReviewedAt(Instant.now());
            
            DnaProposal saved = proposalRepository.save(proposal);
            auditService.log(actor, "PUBLISH", "dna_proposal", id,
                String.format("{\"reviewedBy\":%s}", JsonHelpers.jsonString(actualReviewer)));
            return saved;
        });
    }

    /**
     * DWP-050: a separation-of-duties breach routes the publish to the admin
     * broadcast (ASK-055) — the proposal stays in review and an admin publishes it.
     */
    private void routeSodPublishToAdmin(DnaProposal proposal) {
        try {
            Map<String, Object> payloadMap = new LinkedHashMap<>();
            payloadMap.put("proposalId", proposal.getId());
            payloadMap.put("proposalKind", proposal.getKind());
            payloadMap.put("domainId", proposal.getDomainId());
            payloadMap.put("reason", "separation_of_duties");
            payloadMap.put("proposedBy", proposal.getProposedBy());
            String payload = JsonHelpers.toJson(payloadMap, objectMapper);
            long deadlineSeconds = askService.deriveDeadlineFromTier("standard");
            if (deadlineSeconds > Defaults.MAX_DEADLINE_SECONDS) deadlineSeconds = Defaults.MAX_DEADLINE_SECONDS;
            askService.create("approval", "system", OffboardingWalkService.ADMIN_BROADCAST,
                payload, "standard", "deny", 1,
                Instant.now().plusSeconds(deadlineSeconds), null, null);
            auditService.logSystem("SOD_ROUTE_TO_ADMIN", "dna_proposal", proposal.getId(),
                String.format("{\"reason\":\"separation_of_duties\",\"proposer\":%s}", JsonHelpers.jsonString(proposal.getProposedBy())));
        } catch (Exception e) {
            auditService.logSystem("SOD_ROUTE_TO_ADMIN_FAIL", "dna_proposal", proposal.getId(),
                JsonHelpers.toJson(Map.of("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), objectMapper));
        }
    }

    /**
     * DWP-042: Detect contradictions between the proposal payload and current domain state.
     * Covers rule supersedence chain integrity and goal domain alignment.
     */
    private List<String> detectContradictions(DnaProposal proposal) {
        List<String> issues = new ArrayList<>();
        if (proposal.getDomainId() == null || proposal.getDomainId().isBlank()) {
            return issues;
        }
        try {
            JsonNode payload = objectMapper.readTree(proposal.getPayload());
            String kind = proposal.getKind();

            if ("rule".equals(kind)) {
                if (payload.has("supersedes_id") && !payload.get("supersedes_id").isNull()) {
                    String supersedesId = payload.get("supersedes_id").asText();
                    // DGV-025: Check supersedence chain integrity — refuse if the target
                    // already has a live superseder (fork), or if the target itself is not
                    // found (reference to a non-existent rule).
                    Optional<DnaRule> supersedesOpt = ruleRepository.findById(supersedesId);
                    if (supersedesOpt.isEmpty()) {
                        issues.add("Supersedes reference not found: " + supersedesId);
                    } else {
                        // DWP-041: target must be in a live state, not retired/lapsed
                        String status = supersedesOpt.get().getStatus();
                        if (status != null && !"active".equals(status) && !"superseded".equals(status)) {
                            issues.add("Cannot supersede non-active rule (" + status + "): " + supersedesId);
                        }
                        List<DnaRule> successors = ruleRepository.findBySupersedesId(supersedesId);
                        if (!successors.isEmpty()) {
                            issues.add("Supersession chain fork: " + supersedesId + " already has a live superseder");
                        }
                    }
                }
                // DWP-063: window ordering
                if (payload.has("effective_from") && payload.has("effective_to")
                        && !payload.get("effective_from").isNull() && !payload.get("effective_to").isNull()) {
                    Instant from = JsonHelpers.parseOptionalInstant(payload.get("effective_from").asText(), "effective_from");
                    Instant to = JsonHelpers.parseOptionalInstant(payload.get("effective_to").asText(), "effective_to");
                    if (from != null && to != null && to.isBefore(from)) {
                        issues.add("effective_to must not be before effective_from");
                    }
                }
            }

            if ("goal".equals(kind)) {
                if (payload.has("domain_id") && !payload.get("domain_id").isNull()) {
                    String goalDomainId = payload.get("domain_id").asText();
                    if (!goalDomainId.equals(proposal.getDomainId())) {
                        issues.add("Goal domain_id mismatch: payload domain " + goalDomainId
                            + " differs from proposal domain " + proposal.getDomainId());
                    }
                }
                if (payload.has("effective_from") && payload.has("effective_to")
                        && !payload.get("effective_from").isNull() && !payload.get("effective_to").isNull()) {
                    Instant from = JsonHelpers.parseOptionalInstant(payload.get("effective_from").asText(), "effective_from");
                    Instant to = JsonHelpers.parseOptionalInstant(payload.get("effective_to").asText(), "effective_to");
                    if (from != null && to != null && to.isBefore(from)) {
                        issues.add("effective_to must not be before effective_from");
                    }
                }
            }

            // ASK-053 / DWP-063: Quorum bounds check
            if (payload.has("quorum") && !payload.get("quorum").isNull()) {
                int q = payload.get("quorum").asInt();
                if (q < 1) {
                    issues.add("quorum must be at least 1, found: " + q);
                }
            }
        } catch (Exception e) {
            issues.add("Payload parse error: " + e.getMessage());
        }
        return issues;
    }

    /**
     * DWP-020: Check for proposals whose review SLA has been breached and escalate to admin.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void checkAndEscalateBreachedProposals() {
        Instant now = Instant.now();
        List<DnaProposal> openProposals = proposalRepository.findAllOpen();
        // Purge alreadyEscalated of proposals that are no longer open (resolved/withdrawn)
        // to prevent unbounded set growth and re-escalation of new proposals with same ID pattern.
        alreadyEscalated.removeIf(id -> {
            Optional<DnaProposal> p = proposalRepository.findById(id);
            return p.isEmpty() || !"open".equals(p.get().getStatus());
        });
        for (DnaProposal proposal : openProposals) {
            if (alreadyEscalated.contains(proposal.getId())) {
                continue;
            }
            boolean shouldEscalate = false;
            if (proposal.getDomainId() == null || proposal.getDomainId().isBlank()) {
                // Org-scoped: check against global default SLA from governance settings
                Integer defaultSlaDays = governanceService.getSetting("summa.dna.default-review-sla-days", Integer.class);
                long slaSeconds = (defaultSlaDays != null ? defaultSlaDays : 7) * 86400L;
                if (proposal.getCreatedAt() != null && now.isAfter(proposal.getCreatedAt().plusSeconds(slaSeconds))) {
                    shouldEscalate = true;
                }
            } else {
                Optional<DnaDomain> domainOpt = domainService.findById(proposal.getDomainId());
                if (domainOpt.isPresent()) {
                    DnaDomain domain = domainOpt.get();
                    long slaSeconds = (domain.getReviewSlaDays() != null ? domain.getReviewSlaDays() : 7) * 86400L;
                    if (proposal.getCreatedAt() == null) continue;
                    if (now.isAfter(proposal.getCreatedAt().plusSeconds(slaSeconds))) {
                        shouldEscalate = true;
                    }
                }
            }
            if (shouldEscalate) {
                escalateToAdmin(proposal);
                alreadyEscalated.add(proposal.getId());
            }
        }
    }

    /**
     * DWP-020: Escalate a breached proposal to the admin broadcast.
     */
    private void escalateToAdmin(DnaProposal proposal) {
        try {
            Map<String, Object> payloadMap = new LinkedHashMap<>();
            payloadMap.put("proposalId", proposal.getId());
            payloadMap.put("proposalKind", proposal.getKind());
            payloadMap.put("domainId", proposal.getDomainId());
            payloadMap.put("breachDays", Duration.between(proposal.getCreatedAt(), Instant.now()).toDays());
            String payload = JsonHelpers.toJson(payloadMap, objectMapper);
            // Use critical-tier deadline for escalation to ensure urgency
            long deadlineSeconds = askService.deriveDeadlineFromTier("critical");
            if (deadlineSeconds > Defaults.MAX_DEADLINE_SECONDS) deadlineSeconds = Defaults.MAX_DEADLINE_SECONDS;
            askService.create("question", "system", OffboardingWalkService.ADMIN_BROADCAST,
                payload, "critical", "escalate", 1,
                Instant.now().plusSeconds(deadlineSeconds), null, null);
            auditService.logSystem("PROPOSAL_SLA_BREACH_ESCALATED", "dna_proposal", proposal.getId(),
                String.format("{\"domainId\":%s,\"breachDays\":%d}", JsonHelpers.jsonString(proposal.getDomainId()),
                    Duration.between(proposal.getCreatedAt(), Instant.now()).toDays()));
        } catch (Exception e) {
            auditService.logSystem("PROPOSAL_SLA_BREACH_ESCALATE_FAIL", "dna_proposal", proposal.getId(),
                JsonHelpers.toJson(Map.of("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), objectMapper));
        }
    }

    @Transactional
    public DnaProposal reject(String id, String reviewedBy, String actor) {
        DnaProposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found: " + id));

        if (!proposal.isOpen()) {
            throw new IllegalStateException("Only open proposals can be rejected");
        }
        // DWP-003: the reject door belongs to the domain owner (review_by) or an admin.
        requireReviewAuthority(proposal, actor);
        proposal.setStatus("rejected");
        proposal.setReviewedBy(reviewedBy);
        proposal.setReviewedAt(Instant.now());
        
        DnaProposal saved = proposalRepository.save(proposal);
        auditService.log(actor, "REJECT", "dna_proposal", id, null);
        return saved;
    }

    @Transactional
    public DnaProposal withdraw(String id, String actor) {
        DnaProposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found: " + id));

        // DWP-051: withdrawal is for a pending proposal — a published or rejected
        // one is settled history, not a pending item the proposer can retract.
        if (!proposal.isOpen()) {
            throw new IllegalStateException("Only open proposals can be withdrawn: " + proposal.getStatus());
        }

        if (!proposedByMatches(proposal, actor)) {
            throw new IllegalArgumentException("Only the proposer can withdraw");
        }
        
        proposal.setStatus("withdrawn");
        DnaProposal saved = proposalRepository.save(proposal);
        auditService.log(actor, "WITHDRAW", "dna_proposal", id, null);
        return saved;
    }

    /**
     * DWP-030: amendments come from the proposer or the reviewing owner (as
     * suggested changes) — any other member is refused.
     */
    @Transactional
    public DnaProposal amend(String id, String payload, String actor) {
        DnaProposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found: " + id));

        return domainLockService.withLock(proposal.getDomainId(), () -> {
            if (!proposal.isOpen()) {
                throw new IllegalStateException("Only open proposals can be amended");
            }
            requireAmendAuthority(proposal, actor);

            // SEC-030 / DWP-060: secrets scan on amended payload
            ScanUtils.scanForSecrets(payload, actor, "dna_proposal", id, secretsScanner, auditService);

            proposal.setRevision((proposal.getRevision() != null ? proposal.getRevision() : 0) + 1);
            proposal.setPayload(payload);
            
            DnaProposal saved = proposalRepository.save(proposal);
            auditService.log(actor, "AMEND", "dna_proposal", id, 
                String.format("{\"revision\":%d}", proposal.getRevision()));
            return saved;
        });
    }

    /**
     * DWP-003: the review door belongs to the proposal's reviewing owner
     * ({@code review_by}) or an admin.
     */
    private void requireReviewAuthority(DnaProposal proposal, String actor) {
        if (memberService.isAdmin(actor)) return;
        String reviewBy = proposal.getReviewBy();
        if (reviewBy != null && !reviewBy.isBlank()
                && reviewBy.equals(JsonHelpers.stripIdPrefix(actor))) {
            return;
        }
        throw new IllegalStateException("Only the reviewing owner or an admin may review this proposal");
    }

    /**
     * DWP-030: the proposer or the reviewing owner may amend.
     */
    private void requireAmendAuthority(DnaProposal proposal, String actor) {
        if (proposedByMatches(proposal, actor)) return;
        if (memberService.isAdmin(actor)) return;
        String reviewBy = proposal.getReviewBy();
        if (reviewBy != null && !reviewBy.isBlank()
                && reviewBy.equals(JsonHelpers.stripIdPrefix(actor))) {
            return;
        }
        throw new IllegalStateException("Only the proposer or the reviewing owner may amend this proposal");
    }

    private boolean proposedByMatches(DnaProposal proposal, String actor) {
        String proposedBy = proposal.getProposedBy();
        // DAT-120: proposed_by is a keyed union (h:/a:) and the actor is a bare id —
        // compare the stripped forms so both spellings count as one principal.
        if (proposedBy == null) return false;
        String proposedClean = JsonHelpers.stripIdPrefix(proposedBy);
        String actorClean = JsonHelpers.stripIdPrefix(actor);
        return proposedClean != null && proposedClean.equals(actorClean);
    }
}
