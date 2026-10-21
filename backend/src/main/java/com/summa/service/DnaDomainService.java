package com.summa.service;

import com.summa.repository.DnaDomainRepository;
import com.summa.model.DnaDomain;
import com.summa.repository.DnaCardRepository;
import com.summa.model.DnaCard;
import com.summa.repository.DnaProposalRepository;
import com.summa.model.DnaProposal;
import com.summa.repository.WorkspaceRepository;
import com.summa.repository.DnaRuleRepository;
import com.summa.model.DnaRule;
import com.summa.repository.DnaGlossaryRepository;
import com.summa.model.DnaGlossary;
import com.summa.repository.DnaGoalRepository;
import com.summa.model.DnaGoal;
import com.summa.repository.DnaDecisionRepository;
import com.summa.repository.DataHoldRepository;
import com.summa.repository.AskRepository;
import com.summa.model.DnaDecision;
import com.summa.model.Workspace;
import com.summa.model.Ask;
import com.summa.repository.InitiativeRepository;
import com.summa.model.Initiative;
import com.summa.enums.AskStatus;
import com.summa.constants.Defaults;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import com.summa.exception.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.summa.util.JsonHelpers;

@Service
public class DnaDomainService {
    private static final Logger log = LoggerFactory.getLogger(DnaDomainService.class);
    private final DnaDomainRepository domainRepository;
    private final DnaCardRepository cardRepository;
    private final DnaProposalRepository proposalRepository;
    private final WorkspaceRepository workspaceRepository;
    private final DnaRuleRepository ruleRepository;
    private final DnaGlossaryRepository glossaryRepository;
    private final DnaGoalRepository goalRepository;
    private final DnaDecisionRepository decisionRepository;
    private final DataHoldRepository dataHoldRepository;
    private final AskRepository askRepository;
    private final InitiativeRepository initiativeRepository;
    private final AuditService auditService;
    private final AskService askService;
    private final ObjectMapper objectMapper;

    public DnaDomainService(DnaDomainRepository domainRepository, DnaCardRepository cardRepository,
                            DnaProposalRepository proposalRepository, WorkspaceRepository workspaceRepository,
                            DnaRuleRepository ruleRepository, DnaGlossaryRepository glossaryRepository,
                            DnaGoalRepository goalRepository, DnaDecisionRepository decisionRepository,
                            DataHoldRepository dataHoldRepository,
                            AskRepository askRepository, InitiativeRepository initiativeRepository,
                            AuditService auditService, AskService askService, ObjectMapper objectMapper) {
        this.domainRepository = domainRepository;
        this.cardRepository = cardRepository;
        this.proposalRepository = proposalRepository;
        this.workspaceRepository = workspaceRepository;
        this.ruleRepository = ruleRepository;
        this.glossaryRepository = glossaryRepository;
        this.goalRepository = goalRepository;
        this.decisionRepository = decisionRepository;
        this.dataHoldRepository = dataHoldRepository;
        this.askRepository = askRepository;
        this.initiativeRepository = initiativeRepository;
        this.auditService = auditService;
        this.askService = askService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DnaDomain create(String id, String name, String ownerHumanId, String access,
                            String store, String sod, Integer reviewSlaDays, String residency, String actor) {
        DnaDomain domain = new DnaDomain();
        domain.setId(id);
        domain.setName(name);
        domain.setOwnerHumanId(ownerHumanId);
        domain.setAccess(access != null ? access : "public");
        domain.setStore(store != null ? store : "git");
        // DGV-005: `sod` is a structural attribute, settled at creation (the door
        // that owns it — there is no later structural-update endpoint).
        domain.setSod(sod != null ? sod : "off");
        domain.setReviewSlaDays(reviewSlaDays != null ? reviewSlaDays : 7);
        domain.setResidency(residency);

        DnaDomain saved = domainRepository.save(domain);
        auditService.log(actor != null ? actor : "system", "CREATE", "dna_domain", id,
            String.format("{\"name\":%s,\"access\":%s,\"store\":%s}", JsonHelpers.jsonString(name), JsonHelpers.jsonString(domain.getAccess()), JsonHelpers.jsonString(domain.getStore())));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<DnaDomain> findById(String id) {
        return domainRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<DnaDomain> findByName(String name) {
        return domainRepository.findByNameNotArchived(name);
    }

    @Transactional(readOnly = true)
    public List<DnaDomain> findAll() {
        return domainRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public List<DnaDomain> findAllIncludingArchived() {
        return domainRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DnaDomain> findAllIncludingArchived(int limit) {
        return domainRepository.findAllOrdered(limit);
    }

    @Transactional
    public DnaDomain archive(String id, String actor) {
        DnaDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Domain not found: " + id));

        // DGV-017: legal holds freeze removal, not addition — an archive of a held
        // domain queues behind the hold's release. Rename and merge-into stay open.
        if (dataHoldRepository.existsByKindAndSubjectIdAndReleasedAtIsNull("domain", id)) {
            throw new IllegalStateException("Domain is under a legal hold — archive refused");
        }

        // DGV-040: Archive refuses a domain still holding live-set state
        // (terminal history — superseded/lapsed rules, terminal goals, retired cards,
        // glossary — never blocks; only the live set does, §4.4). A `breached`
        // proposal is still in review (the breach is an urgency flag, not a
        // lifecycle), so it blocks the archive exactly as an `open` one does.
        Instant now = Instant.now();
        long liveCards = cardRepository.countByDomainIdAndStatusNot(id, "retired");
        long openProposals = proposalRepository.countOpenByDomain(id);
        long liveBindings = workspaceRepository.countByDomainIdsContaining(id);
        long activeGoals = goalRepository.countLiveByDomainId(id, now);
        long activeRules = ruleRepository.countLiveByDomainId(id, now);
        long liveGlossary = glossaryRepository.countByDomainIdAndStatusNot(id, "retired");
        if (liveCards > 0 || openProposals > 0 || liveBindings > 0 || activeGoals > 0
                || activeRules > 0 || liveGlossary > 0) {
            throw new IllegalStateException(
                String.format("Domain holds live state: cards=%d proposals=%d bindings=%d goals=%d rules=%d glossary=%d",
                    liveCards, openProposals, liveBindings, activeGoals, activeRules, liveGlossary));
        }

        // DGV-047: archive has no resulting owner to re-key onto, so it settles
        // instead — owner-addressed asks pending against the archiving domain close
        // with an audit note inside the event (ASK-044).
        settleOwnerAddressedAsks(domain, actor);

        domain.setStatus("archived");
        DnaDomain saved = domainRepository.save(domain);
        auditService.log(actor, "ARCHIVE", "dna_domain", id, null);
        return saved;
    }

    /**
     * DGV-047/ASK-044: settle the asks whose addressee was derived from the
     * archiving domain's owner. The ask closes with an audit note inside the
     * archive event; an ask whose expiry behavior escalates or reassigns opens a
     * linked successor to the admin broadcast — the domain's owner is gone, so the
     * terminal hop is the admin set (ASK-055).
     */
    private void settleOwnerAddressedAsks(DnaDomain domain, String actor) {
        String owner = domain.getOwnerHumanId();
        if (owner == null || owner.isBlank()) return;
        // The owner is looked up in both the bare and the keyed-union spelling;
        // an ask is settled once, however many spellings resolve to it.
        java.util.Set<String> settled = new java.util.HashSet<>();
        for (String oldForm : List.of(owner, "h:" + JsonHelpers.stripIdPrefix(owner))) {
            for (Ask ask : askRepository.findByToAndStatusPending(oldForm)) {
                if (ask.getId() != null && !settled.add(ask.getId())) continue;
                if (!isAskScopedToDomain(ask, domain.getId())) continue;
                String behavior = ask.getExpiryBehavior() != null ? ask.getExpiryBehavior() : "deny";
                ask.setStatus(AskStatus.EXPIRED.getValue());
                askRepository.save(ask);
                auditService.log(actor, "ARCHIVE_SETTLE_ASK", "ask", ask.getId(),
                    String.format("{\"domainId\":%s,\"behavior\":%s,\"reason\":%s}",
                        JsonHelpers.jsonString(domain.getId()),
                        JsonHelpers.jsonString(behavior),
                        JsonHelpers.jsonString("owner_addressed_ask_settled_on_domain_archive")));
                if ("escalate".equals(behavior) || "reassign".equals(behavior)) {
                    try {
                        long deadlineSeconds = Math.min(
                            askService.deriveDeadlineFromTier(ask.getSlaTier()),
                            Defaults.MAX_DEADLINE_SECONDS);
                        askService.create(ask.getKind(), ask.getFrom(),
                            OffboardingWalkService.ADMIN_BROADCAST, ask.getPayload(),
                            ask.getSlaTier(), ask.getExpiryBehavior(), ask.getQuorumRequired(),
                            Instant.now().plusSeconds(deadlineSeconds),
                            ask.getInitiativeId(), ask.getWorkspaceId());
                        auditService.log(actor, "ARCHIVE_ASK_SUCCESSOR", "ask", ask.getId(),
                            String.format("{\"domainId\":%s}", JsonHelpers.jsonString(domain.getId())));
                    } catch (Exception e) {
                        auditService.logSystem("ARCHIVE_ASK_SUCCESSOR_FAIL", "ask", ask.getId(),
                            String.format("{\"domainId\":%s,\"error\":%s}",
                                JsonHelpers.jsonString(domain.getId()),
                                JsonHelpers.jsonString(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
                    }
                }
            }
        }
    }

    @Transactional
    public DnaDomain rename(String id, String newName, String actor) {
        DnaDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Domain not found: " + id));

        domain.setName(newName);
        DnaDomain saved = domainRepository.save(domain);
        auditService.log(actor, "RENAME", "dna_domain", id,
            String.format("{\"newName\":%s}", JsonHelpers.jsonString(newName)));
        return saved;
    }

    @Transactional
    public DnaDomain updateOwner(String id, String newOwnerId, String actor) {
        DnaDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Domain not found: " + id));
        String previousOwner = domain.getOwnerHumanId();
        domain.setOwnerHumanId(newOwnerId);
        DnaDomain saved = domainRepository.save(domain);
        // DWP-021: owner re-pointing re-keys the queue's rendering — the review
        // authority is the proposal's review_by, so without this the new owner is
        // refused at the publish door while the former owner keeps it.
        rekeyReviewAuthority(id, newOwnerId);
        // DGV-045: the owner re-point is one of the owner-re-pointing doors the
        // attention remapping names — pending asks whose addressee was derived
        // from the domain's owner re-key to the resulting owner, ids stable and
        // deadlines untouched.
        rekeyOwnerAddressedAsks(previousOwner, newOwnerId, id, actor);
        auditService.log(actor, "UPDATE_OWNER", "dna_domain", id,
            String.format("{\"newOwner\":%s}", JsonHelpers.jsonString(newOwnerId)));
        return saved;
    }

    /**
     * DWP-021: the queue belongs to the domain — owner re-pointing re-keys the
     * rendering with the review_by clocks untouched.
     */
    private void rekeyReviewAuthority(String domainId, String newOwnerId) {
        if (newOwnerId == null || newOwnerId.isBlank()) return;
        for (DnaProposal proposal : proposalRepository.findOpenByDomain(domainId)) {
            proposal.setReviewBy(newOwnerId);
            proposalRepository.save(proposal);
        }
    }

    @Transactional
    public DnaDomain updateAccess(String id, String access, String actor) {
        DnaDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Domain not found: " + id));
        domain.setAccess(access);
        DnaDomain saved = domainRepository.save(domain);
        auditService.log(actor, "UPDATE_ACCESS", "dna_domain", id, null);
        return saved;
    }

    /**
     * DWP-025: A domain edit re-derives standing clocks in one direction only —
     * tightening recomputes each open proposal's review deadline from its filed date
     * under the new SLA and applies it only where it lands earlier; loosening
     * leaves standing clocks untouched. If tightening would push a proposal's
     * review deadline into the past, the proposal is flagged as already breached
     * (status `breached`) rather than retroactively assigned a past deadline.
     * The `breached` status is a rendering urgency flag, not a lifecycle: the
     * proposal stays open, and the scheduled SLA checker re-escalates it.
     */
    @Transactional
    public DnaDomain updateReviewSlaDays(String id, Integer reviewSlaDays, String actor) {
        if (reviewSlaDays == null || reviewSlaDays < 1) {
            throw new IllegalArgumentException("reviewSlaDays must be >= 1");
        }
        DnaDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Domain not found: " + id));
        Integer oldSlaDays = domain.getReviewSlaDays() != null ? domain.getReviewSlaDays() : 7;
        if (!reviewSlaDays.equals(oldSlaDays)) {
            domain.setReviewSlaDays(reviewSlaDays);
            domainRepository.save(domain);
            // DWP-025: only tighten — when the new SLA is shorter, re-derive standing clocks
            if (reviewSlaDays < oldSlaDays) {
                rederiveReviewClocks(id, reviewSlaDays);
            }
        }
        auditService.log(actor, "UPDATE_REVIEW_SLA", "dna_domain", id,
            String.format("{\"oldSlaDays\":%d,\"newSlaDays\":%d}", oldSlaDays, reviewSlaDays));
        return domain;
    }

    private void rederiveReviewClocks(String domainId, int newSlaDays) {
        long newSeconds = newSlaDays * 86400L;
        for (DnaProposal p : proposalRepository.findOpenByDomain(domainId)) {
            if (p.getCreatedAt() == null) continue;
            long newDeadline = p.getCreatedAt().getEpochSecond() + newSeconds;
            long now = java.time.Instant.now().getEpochSecond();
            // DWP-025: flag already-breached rather than retroactively assigning a past deadline.
            // The proposal stays open; the scheduled SLA checker re-escalates it.
            if (newDeadline < now) {
                p.setStatus("breached");
                proposalRepository.save(p);
                auditService.logSystem("SLA_TIGHTEN_BREACHED", "dna_proposal", p.getId(),
                    String.format("{\"newSlaDays\":%d,\"filedAt\":%d}", newSlaDays, p.getCreatedAt().getEpochSecond()));
            }
        }
    }

    // DGV-010..013: Split a domain into a child domain.
    // The mapping is total: every item, workspace binding, and open proposal names its result.
    // The emptied parent archives inside the same audited event.
    // Each result inherits undeclared attributes from the parent.
    @Transactional
    public List<DnaDomain> split(String parentId, String actor,
                                  String ownerHumanId, String access, String store,
                                  String sod, String residency,
                                  String namedReaders,
                                   List<String> itemIds, List<String> workspaceIds,
                                   List<String> proposalIds) {
        if (itemIds == null) itemIds = List.of();
        if (workspaceIds == null) workspaceIds = List.of();
        if (proposalIds == null) proposalIds = List.of();
        // Deduplicate to prevent cardinality arithmetic from being gamed by duplicate IDs.
        itemIds = itemIds.stream().distinct().toList();
        workspaceIds = workspaceIds.stream().distinct().toList();
        proposalIds = proposalIds.stream().distinct().toList();
        DnaDomain parent = domainRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("Domain not found: " + parentId));

        // DGV-017: refuse while under a kind-domain hold
        if (dataHoldRepository.existsByKindAndSubjectIdAndReleasedAtIsNull("domain", parentId)) {
            throw new IllegalStateException("Domain is under a legal hold — split refused");
        }

        // DGV-040/DGV-012: refuse if parent holds live state not mapped below.
        // Every live item class counts — a card, a rule, a goal, a glossary entry —
        // and every open proposal and live binding must be named too. Decisions
        // never block (DNC-032: they are history at birth) and ride the parent's
        // archive as resolvable history.
        Instant now = Instant.now();
        long liveCards = cardRepository.countByDomainIdAndStatusNot(parentId, "retired");
        long liveRules = ruleRepository.countLiveByDomainId(parentId, now);
        long liveGoals = goalRepository.countLiveByDomainId(parentId, now);
        long liveGlossary = glossaryRepository.countByDomainIdAndStatusNot(parentId, "retired");
        long liveProposals = proposalRepository.countOpenByDomain(parentId);
        long liveBindings = workspaceRepository.countByDomainIdsContaining(parentId);
        long liveItems = liveCards + liveRules + liveGoals + liveGlossary;
        if (Math.max(0, liveItems - itemIds.size()) > 0 || Math.max(0, liveProposals - proposalIds.size()) > 0 || Math.max(0, liveBindings - workspaceIds.size()) > 0) {
            throw new IllegalStateException(
                String.format("Split mapping is not total: unmapped_items=%d proposals=%d bindings=%d",
                    Math.max(0, liveItems - itemIds.size()), Math.max(0, liveProposals - proposalIds.size()), Math.max(0, liveBindings - workspaceIds.size())));
        }

        // DGV-011: create child inheriting undeclared parent attrs
        DnaDomain child = new DnaDomain();
        child.setId(UUID.randomUUID().toString());
        child.setName(parent.getName() + " (split)");
        child.setOwnerHumanId(ownerHumanId != null ? ownerHumanId : parent.getOwnerHumanId());
        child.setAccess(access != null ? access : parent.getAccess());
        child.setStore(store != null ? store : parent.getStore());
        child.setSod(sod != null ? sod : parent.getSod());
        child.setReviewSlaDays(parent.getReviewSlaDays());
        child.setResidency(residency != null ? residency : parent.getResidency());
        child.setNamedReaders(namedReaders != null ? namedReaders : parent.getNamedReaders());
        child.setStatus("active");
        DnaDomain savedChild = domainRepository.save(child);

        // Validate all referenced IDs exist and belong to the parent domain before mutating anything.
        // This prevents partial splits where items are silently left behind.
        List<String> missingCards = new ArrayList<>();
        List<String> missingItems = new ArrayList<>();
        for (String cardId : itemIds) {
            Optional<DnaCard> cardOpt = cardRepository.findById(cardId);
            if (cardOpt.isPresent()) {
                DnaCard card = cardOpt.get();
                if (card.getDomainId() == null || !card.getDomainId().equals(parent.getId())) {
                    missingCards.add(cardId + "(wrong_domain:" + card.getDomainId() + ")");
                }
                continue;
            }
            // DGV-010/DGV-012: the mapping names items of every class the domain
            // holds — a rule, a goal, or a glossary entry moves with the same
            // "ids stable" rule a card does.
            String misplaced = misplacedItemDomain(cardId, parent.getId());
            if (misplaced != null) {
                missingItems.add(cardId + "(" + misplaced + ")");
            }
        }
        List<String> missingProposals = new ArrayList<>();
        for (String propId : proposalIds) {
            Optional<DnaProposal> propOpt = proposalRepository.findById(propId);
            if (propOpt.isEmpty()) {
                missingProposals.add(propId);
                continue;
            }
            DnaProposal prop = propOpt.get();
            if (prop.getDomainId() == null || !prop.getDomainId().equals(parent.getId())) {
                missingProposals.add(propId + "(wrong_domain:" + prop.getDomainId() + ")");
            }
        }
        List<String> missingWorkspaces = new ArrayList<>();
        for (String wsId : workspaceIds) {
            Optional<Workspace> wsOpt = workspaceRepository.findById(wsId);
            if (wsOpt.isEmpty()) {
                missingWorkspaces.add(wsId);
                continue;
            }
            Workspace ws = wsOpt.get();
            if (ws.getDomainIds() == null || ws.getDomainIds().isBlank() || ws.getDomainIds().equals("[]")) {
                missingWorkspaces.add(wsId + "(no_domains)");
                continue;
            }
            try {
                List<String> domains = objectMapper.readValue(ws.getDomainIds(), new TypeReference<List<String>>() {});
                if (!domains.contains(parent.getId())) {
                    missingWorkspaces.add(wsId + "(wrong_domain:" + domains + ")");
                }
            } catch (Exception e) {
                missingWorkspaces.add(wsId + "(parse_error)");
            }
        }
        List<String> allErrors = new ArrayList<>();
        if (!missingCards.isEmpty()) allErrors.add("missing_or_misplaced_cards=" + String.join(",", missingCards));
        if (!missingItems.isEmpty()) allErrors.add("missing_or_misplaced_items=" + String.join(",", missingItems));
        if (!missingProposals.isEmpty()) allErrors.add("missing_or_misplaced_proposals=" + String.join(",", missingProposals));
        if (!missingWorkspaces.isEmpty()) allErrors.add("missing_or_misplaced_workspaces=" + String.join(",", missingWorkspaces));
        if (!allErrors.isEmpty()) {
            // Roll back the child domain we just created
            child.setStatus("archived");
            domainRepository.save(child);
            throw new IllegalStateException(
                "Split mapping validation failed: " + String.join("; ", allErrors));
        }

        // Move the mapped items (ids stable per DGV-010) — a card, a rule, a goal,
        // or a glossary entry all move to the child by re-pointing their domain.
        for (String itemId : itemIds) {
            moveItemToDomain(itemId, savedChild.getId());
        }

        // Remap proposals (ids stable per DGV-042) — validate each belongs to parent domain
        for (String propId : proposalIds) {
            proposalRepository.findById(propId).ifPresent(prop -> {
                prop.setDomainId(savedChild.getId());
                proposalRepository.save(prop);
            });
        }

        // Remap workspace bindings — validate each workspace is bound to parent domain
        for (String wsId : workspaceIds) {
            workspaceRepository.findById(wsId).ifPresent(ws -> {
                try {
                    List<String> domains = objectMapper.readValue(ws.getDomainIds(), new TypeReference<List<String>>() {});
                    domains.remove(parent.getId());
                    domains.add(savedChild.getId());
                    ws.setDomainIds(objectMapper.writeValueAsString(domains));
                    workspaceRepository.save(ws);
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid workspace domain_ids: " + e.getMessage());
                }
            });
        }

        // Re-key asks addressed to the old owner (DGV-045)
        rekeyOwnerAdds(parentId, savedChild.getId(), actor);

        // Archive the emptied parent (division is dissolve-by-split, DGV-012)
        parent.setStatus("archived");
        domainRepository.save(parent);

        auditService.log(actor, "SPLIT", "dna_domain", parentId,
            String.format("{\"childId\":%s}", JsonHelpers.jsonString(savedChild.getId())));
        return Collections.singletonList(savedChild);
    }

    // DGV-014: Merge source into survivor.
    // Access defaults to the narrower of the pair; undeclared attrs persist from survivor.
    // Merge never silently widens access.
    @Transactional
    public DnaDomain merge(String sourceId, String survivorId, String actor,
                             String declaredAccess, String declaredNamedReaders) {
        if (sourceId.equals(survivorId)) {
            throw new IllegalStateException("Cannot merge a domain onto itself");
        }
        DnaDomain source = domainRepository.findById(sourceId)
                .orElseThrow(() -> new EntityNotFoundException("Source domain not found: " + sourceId));
        DnaDomain survivor = domainRepository.findById(survivorId)
                .orElseThrow(() -> new EntityNotFoundException("Survivor domain not found: " + survivorId));

        // DGV-017: refuse while either side is under a kind-domain hold
        if (dataHoldRepository.existsByKindAndSubjectIdAndReleasedAtIsNull("domain", sourceId)) {
            throw new IllegalStateException("Source domain is under a legal hold — merge refused");
        }
        if (dataHoldRepository.existsByKindAndSubjectIdAndReleasedAtIsNull("domain", survivorId)) {
            throw new IllegalStateException("Survivor domain is under a legal hold — merge refused");
        }

        // DGV-014: compute default access — narrower wins; refuse if no strictly smaller side
        String resolvedAccess = declaredAccess != null ? declaredAccess : computeMergeAccess(source, survivor);
        if (resolvedAccess == null) {
            throw new IllegalStateException(
                "Merge access default cannot be computed — both sides admit different members; declare access");
        }

        // Move cards (ids stable)
        for (DnaCard card : cardRepository.findByDomainId(sourceId)) {
            card.setDomainId(survivorId);
            cardRepository.save(card);
        }

        // Move rules (ids stable)
        for (DnaRule rule : ruleRepository.findByDomainId(sourceId)) {
            rule.setDomainId(survivorId);
            ruleRepository.save(rule);
        }

        // Move decisions (ids stable, lifecycle-free)
        // DNC-032: decisions move with the corpus on merge. The move is a topology
        // re-point, not a content update — the entity's @PreUpdate guard refuses any
        // write to a decision, so the pointer is re-pointed natively and the
        // decision's own fields stay untouched (DNC-030).
        for (DnaDecision decision : decisionRepository.findByDomainId(sourceId)) {
            decisionRepository.updateDomainId(decision.getId(), survivorId);
        }

        // Move glossary (ids stable)
        for (DnaGlossary g : glossaryRepository.findByDomainId(sourceId)) {
            g.setDomainId(survivorId);
            glossaryRepository.save(g);
        }

        // Move goals (ids stable)
        for (DnaGoal goal : goalRepository.findByDomainId(sourceId)) {
            goal.setDomainId(survivorId);
            goalRepository.save(goal);
        }

        // Remap proposals (ids stable per DGV-042) — only open proposals migrate
        for (DnaProposal prop : proposalRepository.findOpenByDomain(sourceId)) {
            prop.setDomainId(survivorId);
            proposalRepository.save(prop);
        }

        // Remap workspace bindings
        List<Workspace> allWorkspaces = workspaceRepository.findAll();
        for (Workspace ws : allWorkspaces) {
            try {
                String domainIdsStr = ws.getDomainIds();
                if (domainIdsStr == null || domainIdsStr.isBlank() || domainIdsStr.equals("[]")) continue;
                List<String> domains = objectMapper.readValue(domainIdsStr, new TypeReference<List<String>>() {});
                if (domains.contains(sourceId)) {
                    domains.remove(sourceId);
                    domains.add(survivorId);
                    ws.setDomainIds(objectMapper.writeValueAsString(domains));
                    workspaceRepository.save(ws);
                }
            } catch (Exception e) {
                auditService.logSystem("MERGE_SKIP_MALFORMED_WS", "workspace", ws.getId(),
                    JsonHelpers.toJson(Map.of("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), objectMapper));
            }
        }

        // Re-key asks (DGV-045)
        rekeyOwnerAdds(sourceId, survivorId, actor);

        // Archive the source
        source.setStatus("archived");
        domainRepository.save(source);

        // Apply resolved access to survivor
        survivor.setAccess(resolvedAccess);
        if (declaredNamedReaders != null) {
            survivor.setNamedReaders(declaredNamedReaders);
        }
        DnaDomain savedSurvivor = domainRepository.save(survivor);

        auditService.log(actor, "MERGE", "dna_domain", survivorId,
            String.format("{\"sourceId\":%s,\"access\":%s}", JsonHelpers.jsonString(sourceId), JsonHelpers.jsonString(resolvedAccess)));
        return savedSurvivor;
    }

    private String computeMergeAccess(DnaDomain source, DnaDomain survivor) {
        // public is widest; any other pairing compares evaluated member sets
        if ("public".equals(source.getAccess()) && "public".equals(survivor.getAccess())) {
            return "public";
        }
        if ("public".equals(source.getAccess()) || "public".equals(survivor.getAccess())) {
            return "public".equals(source.getAccess()) ? survivor.getAccess() : source.getAccess();
        }
        // Both non-public: compare named_readers cardinality; equal → refuse
        int sourceSize = countNamed(source.getNamedReaders());
        int survivorSize = countNamed(survivor.getNamedReaders());
        if (sourceSize < survivorSize) return source.getAccess();
        if (survivorSize < sourceSize) return survivor.getAccess();
        return null; // equal — refuse, caller must declare
    }

    private int countNamed(String namedReadersJson) {
        try {
            List<String> list = objectMapper.readValue(namedReadersJson, new TypeReference<List<String>>() {});
            return list.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * DGV-010/DGV-012: a split's mapping names items of every class the domain
     * holds. This resolves a mapped id to the domain it actually sits in — null
     * when the id names no item at all, and a marker when it names an item of
     * another domain.
     */
    private String misplacedItemDomain(String itemId, String parentId) {
        Optional<DnaRule> ruleOpt = ruleRepository.findById(itemId);
        if (ruleOpt.isPresent()) {
            return ruleOpt.get().getDomainId() != null && ruleOpt.get().getDomainId().equals(parentId)
                ? null : "wrong_domain:" + ruleOpt.get().getDomainId();
        }
        Optional<DnaGoal> goalOpt = goalRepository.findById(itemId);
        if (goalOpt.isPresent()) {
            return goalOpt.get().getDomainId() != null && goalOpt.get().getDomainId().equals(parentId)
                ? null : "wrong_domain:" + goalOpt.get().getDomainId();
        }
        Optional<DnaGlossary> glossaryOpt = glossaryRepository.findById(itemId);
        if (glossaryOpt.isPresent()) {
            return glossaryOpt.get().getDomainId() != null && glossaryOpt.get().getDomainId().equals(parentId)
                ? null : "wrong_domain:" + glossaryOpt.get().getDomainId();
        }
        return "not_an_item";
    }

    /**
     * DGV-010: "items move with ids stable (citations and supersession chains
     * survive)". A split re-points the mapped item's domain; the item's own id and
     * content are untouched, so a citation to it and a supersession chain through
     * it survive the move.
     */
    private void moveItemToDomain(String itemId, String targetDomainId) {
        Optional<DnaCard> cardOpt = cardRepository.findById(itemId);
        if (cardOpt.isPresent()) {
            cardOpt.get().setDomainId(targetDomainId);
            cardRepository.save(cardOpt.get());
            return;
        }
        Optional<DnaRule> ruleOpt = ruleRepository.findById(itemId);
        if (ruleOpt.isPresent()) {
            ruleOpt.get().setDomainId(targetDomainId);
            ruleRepository.save(ruleOpt.get());
            return;
        }
        Optional<DnaGoal> goalOpt = goalRepository.findById(itemId);
        if (goalOpt.isPresent()) {
            goalOpt.get().setDomainId(targetDomainId);
            goalRepository.save(goalOpt.get());
            return;
        }
        Optional<DnaGlossary> glossaryOpt = glossaryRepository.findById(itemId);
        if (glossaryOpt.isPresent()) {
            glossaryOpt.get().setDomainId(targetDomainId);
            glossaryRepository.save(glossaryOpt.get());
        }
    }

    private void rekeyOwnerAdds(String oldDomainId, String newDomainId, String actor) {
        Optional<DnaDomain> oldOpt = domainRepository.findById(oldDomainId);
        Optional<DnaDomain> newOpt = domainRepository.findById(newDomainId);
        if (oldOpt.isEmpty() || newOpt.isEmpty()) return;
        String oldOwner = oldOpt.get().getOwnerHumanId();
        String newOwner = newOpt.get().getOwnerHumanId();
        if (oldOwner == null) return;
        // DAT-120: owner_human_id is a bare humans.id, but asks.to is a keyed union.
        // Query both bare and h:-prefixed forms so asks filed by either code path
        // are re-keyed, and compare owners by their stripped forms.
        if (Objects.equals(JsonHelpers.stripIdPrefix(oldOwner), JsonHelpers.stripIdPrefix(newOwner))) return;
        rekeyOwnerAddressedAsks(oldOwner, newOwner, oldDomainId, actor);
    }

    /**
     * DGV-045: pending asks whose addressee was derived from a domain's owner
     * re-key to the resulting owner — ask ids stable, deadlines untouched. The
     * derived addressee is the domain's owner, so an ask is in scope when it is
     * addressed to that owner and the domain is what the ask hangs off (the
     * workspace it is bound to, or the initiative whose goal lives in the domain).
     */
    private void rekeyOwnerAddressedAsks(String oldOwner, String newOwner, String domainId, String actor) {
        if (oldOwner == null || oldOwner.isBlank()) return;
        if (newOwner == null || newOwner.isBlank()) return;
        if (Objects.equals(JsonHelpers.stripIdPrefix(oldOwner), JsonHelpers.stripIdPrefix(newOwner))) return;
        String newOwnerKeyed = "h:" + JsonHelpers.stripIdPrefix(newOwner);
        java.util.Set<String> rekeyed = new java.util.HashSet<>();
        for (String oldForm : List.of(oldOwner, "h:" + JsonHelpers.stripIdPrefix(oldOwner))) {
            for (Ask ask : askRepository.findByToAndStatusPending(oldForm)) {
                if (ask.getId() != null && !rekeyed.add(ask.getId())) continue;
                boolean scoped = isAskScopedToDomain(ask, domainId);
                if (scoped) {
                    ask.setTo(newOwnerKeyed);
                    askRepository.save(ask);
                    auditService.logSystem("REKEY_ASK", "ask", ask.getId(),
                        String.format("{\"oldDomain\":%s,\"newDomain\":%s,\"oldTo\":%s,\"newTo\":%s}",
                            JsonHelpers.jsonString(domainId), JsonHelpers.jsonString(domainId),
                            JsonHelpers.jsonString(oldForm), JsonHelpers.jsonString(newOwnerKeyed)));
                }
            }
        }
    }

    private boolean isAskScopedToDomain(Ask ask, String domainId) {
        // Validate that the initiative or workspace actually belongs to this domain
        if (ask.getInitiativeId() != null && !ask.getInitiativeId().isBlank()) {
            try {
                Optional<Initiative> initOpt = initiativeRepository.findById(ask.getInitiativeId());
                if (initOpt.isPresent() && initOpt.get().getGoalRef() != null) {
                    Optional<DnaGoal> goalOpt = goalRepository.findById(initOpt.get().getGoalRef());
                    if (goalOpt.isPresent() && domainId.equals(goalOpt.get().getDomainId())) {
                        return true;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to resolve initiative scope for ask {}, falling through to workspace check: {}", ask.getId(), e.getMessage());
            }
        }
        if (ask.getWorkspaceId() != null && !ask.getWorkspaceId().isBlank()) {
            try {
                Optional<Workspace> wsOpt = workspaceRepository.findById(ask.getWorkspaceId());
                if (wsOpt.isPresent() && wsOpt.get().getDomainIds() != null
                        && !wsOpt.get().getDomainIds().isBlank()
                        && !wsOpt.get().getDomainIds().equals("[]")) {
                    List<String> wsDomains = objectMapper.readValue(wsOpt.get().getDomainIds(), new TypeReference<List<String>>() {});
                    return wsDomains.contains(domainId);
                }
            } catch (Exception e) {
                // Fall through
            }
        }
        return false;
    }

    public List<DnaDomain> findByOwnerHumanId(String ownerId) {
        return domainRepository.findByOwnerHumanId(ownerId);
    }
}
