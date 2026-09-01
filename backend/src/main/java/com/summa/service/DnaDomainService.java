package com.summa.service;

import com.summa.repository.DnaDomainRepository;
import com.summa.model.DnaDomain;
import com.summa.repository.DnaCardRepository;
import com.summa.repository.DnaProposalRepository;
import com.summa.repository.WorkspaceRepository;
import com.summa.repository.DnaRuleRepository;
import com.summa.repository.DnaGlossaryRepository;
import com.summa.repository.DnaGoalRepository;
import com.summa.repository.DnaDecisionRepository;
import com.summa.repository.DataHoldRepository;
import com.summa.repository.AskRepository;
import com.summa.model.DnaDecision;
import com.summa.model.Workspace;
import com.summa.model.Ask;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collections;

@Service
public class DnaDomainService {
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
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public DnaDomainService(DnaDomainRepository domainRepository, DnaCardRepository cardRepository,
                            DnaProposalRepository proposalRepository, WorkspaceRepository workspaceRepository,
                            DnaRuleRepository ruleRepository, DnaGlossaryRepository glossaryRepository,
                            DnaGoalRepository goalRepository, DnaDecisionRepository decisionRepository,
                            DataHoldRepository dataHoldRepository,
                            AskRepository askRepository, AuditService auditService, ObjectMapper objectMapper) {
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
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DnaDomain create(String id, String name, String ownerHumanId, String access, 
                            String store, Integer reviewSlaDays, String residency) {
        DnaDomain domain = new DnaDomain();
        domain.setId(id);
        domain.setName(name);
        domain.setOwnerHumanId(ownerHumanId);
        domain.setAccess(access != null ? access : "public");
        domain.setStore(store != null ? store : "git");
        domain.setReviewSlaDays(reviewSlaDays != null ? reviewSlaDays : 7);
        domain.setResidency(residency);
        
        DnaDomain saved = domainRepository.save(domain);
        auditService.log("system", "CREATE", "dna_domain", id, 
            String.format("{\"name\":\"%s\",\"access\":\"%s\",\"store\":\"%s\"}", name, domain.getAccess(), domain.getStore()));
        return saved;
    }

    public Optional<DnaDomain> findById(String id) {
        return domainRepository.findById(id);
    }

    public Optional<DnaDomain> findByName(String name) {
        return domainRepository.findByNameNotArchived(name);
    }

    public List<DnaDomain> findAll() {
        return domainRepository.findAllActive();
    }

    public List<DnaDomain> findAllIncludingArchived() {
        return domainRepository.findAll();
    }

    @Transactional
    public DnaDomain archive(String id, String actor) {
        DnaDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + id));

        // DGV-040: Archive refuses a domain still holding live-set state
        long liveCards = cardRepository.countByDomainIdAndStatusNot(id, "retired");
        long openProposals = proposalRepository.countByDomainIdAndStatus(id, "open");
        long liveBindings = workspaceRepository.countByDomainIdsContaining(id);
        if (liveCards > 0 || openProposals > 0 || liveBindings > 0) {
            throw new IllegalStateException(
                String.format("Domain holds live state: cards=%d proposals=%d bindings=%d",
                    liveCards, openProposals, liveBindings));
        }

        domain.setStatus("archived");
        DnaDomain saved = domainRepository.save(domain);
        auditService.log(actor, "ARCHIVE", "dna_domain", id, null);
        return saved;
    }

    @Transactional
    public DnaDomain rename(String id, String newName, String actor) {
        DnaDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + id));

        domain.setName(newName);
        DnaDomain saved = domainRepository.save(domain);
        auditService.logWithNode(actor, "RENAME", "dna_domain", id, null,
            String.format("{\"newName\":\"%s\"}", newName));
        return saved;
    }

    @Transactional
    public DnaDomain updateOwner(String id, String newOwnerId, String actor) {
        DnaDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + id));
        domain.setOwnerHumanId(newOwnerId);
        DnaDomain saved = domainRepository.save(domain);
        auditService.log(actor, "UPDATE_OWNER", "dna_domain", id, 
            String.format("{\"newOwner\":\"%s\"}", newOwnerId));
        return saved;
    }

    @Transactional
    public DnaDomain updateAccess(String id, String access, String actor) {
        DnaDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + id));
        domain.setAccess(access);
        DnaDomain saved = domainRepository.save(domain);
        auditService.log(actor, "UPDATE_ACCESS", "dna_domain", id, null);
        return saved;
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
        DnaDomain parent = domainRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + parentId));

        // DGV-017: refuse while under a kind-domain hold
        if (dataHoldRepository.existsByKindAndSubjectIdAndReleasedAtIsNull("domain", parentId)) {
            throw new IllegalStateException("Domain is under a legal hold — split refused");
        }

        // DGV-040/DGV-012: refuse if parent holds live state not mapped below
        long liveCards = cardRepository.countByDomainIdAndStatusNot(parentId, "retired");
        long liveProposals = proposalRepository.countByDomainIdAndStatus(parentId, "open");
        long liveBindings = workspaceRepository.countByDomainIdsContaining(parentId);
        if (liveCards - itemIds.size() > 0 || liveProposals - proposalIds.size() > 0 || liveBindings - workspaceIds.size() > 0) {
            throw new IllegalStateException(
                String.format("Split mapping is not total: unmapped_cards=%d proposals=%d bindings=%d",
                    liveCards - itemIds.size(), liveProposals - proposalIds.size(), liveBindings - workspaceIds.size()));
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

        // Move cards (ids stable per DGV-010)
        for (String cardId : itemIds) {
            cardRepository.findById(cardId).ifPresent(card -> {
                card.setDomainId(savedChild.getId());
                cardRepository.save(card);
            });
        }

        // Remap proposals (ids stable per DGV-042)
        for (String propId : proposalIds) {
            proposalRepository.findById(propId).ifPresent(prop -> {
                prop.setDomainId(savedChild.getId());
                proposalRepository.save(prop);
            });
        }

        // Remap workspace bindings
        for (String wsId : workspaceIds) {
            workspaceRepository.findById(wsId).ifPresent(ws -> {
                try {
                    List<String> domains = objectMapper.readValue(ws.getDomainIds(), new TypeReference<List<String>>() {});
                    domains.remove(parentId);
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
            String.format("{\"childId\":\"%s\"}", savedChild.getId()));
        return Collections.singletonList(savedChild);
    }

    // DGV-014: Merge source into survivor.
    // Access defaults to the narrower of the pair; undeclared attrs persist from survivor.
    // Merge never silently widens access.
    @Transactional
    public DnaDomain merge(String sourceId, String survivorId, String actor,
                            String declaredAccess, String declaredNamedReaders) {
        DnaDomain source = domainRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source domain not found: " + sourceId));
        DnaDomain survivor = domainRepository.findById(survivorId)
                .orElseThrow(() -> new IllegalArgumentException("Survivor domain not found: " + survivorId));

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
        for (com.summa.model.DnaCard card : cardRepository.findByDomainId(sourceId)) {
            card.setDomainId(survivorId);
            cardRepository.save(card);
        }

        // Move rules (ids stable)
        for (com.summa.model.DnaRule rule : ruleRepository.findByDomainId(sourceId)) {
            rule.setDomainId(survivorId);
            ruleRepository.save(rule);
        }

        // Move decisions (ids stable, lifecycle-free)
        for (DnaDecision decision : decisionRepository.findByDomainId(sourceId)) {
            decision.setDomainId(survivorId);
            decisionRepository.save(decision);
        }

        // Move glossary (ids stable)
        for (com.summa.model.DnaGlossary g : glossaryRepository.findByDomainId(sourceId)) {
            g.setDomainId(survivorId);
            glossaryRepository.save(g);
        }

        // Move goals (ids stable)
        for (com.summa.model.DnaGoal goal : goalRepository.findByDomainId(sourceId)) {
            goal.setDomainId(survivorId);
            goalRepository.save(goal);
        }

        // Remap proposals (ids stable per DGV-042)
        for (com.summa.model.DnaProposal prop : proposalRepository.findByDomainId(sourceId)) {
            prop.setDomainId(survivorId);
            proposalRepository.save(prop);
        }

        // Remap workspace bindings
        List<Workspace> allWorkspaces = workspaceRepository.findAll();
        for (Workspace ws : allWorkspaces) {
            try {
                List<String> domains = objectMapper.readValue(ws.getDomainIds(), new TypeReference<List<String>>() {});
                if (domains.contains(sourceId)) {
                    domains.remove(sourceId);
                    domains.add(survivorId);
                    ws.setDomainIds(objectMapper.writeValueAsString(domains));
                    workspaceRepository.save(ws);
                }
            } catch (Exception e) {
                // skip malformed
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
            String.format("{\"sourceId\":\"%s\",\"access\":\"%s\"}", sourceId, resolvedAccess));
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

    private void rekeyOwnerAdds(String oldDomainId, String newDomainId, String actor) {
        Optional<DnaDomain> oldOpt = domainRepository.findById(oldDomainId);
        Optional<DnaDomain> newOpt = domainRepository.findById(newDomainId);
        if (oldOpt.isEmpty() || newOpt.isEmpty()) return;
        String oldOwner = oldOpt.get().getOwnerHumanId();
        String newOwner = newOpt.get().getOwnerHumanId();
        if (oldOwner.equals(newOwner)) return;
        // Re-key ALL pending asks addressed to old owner (DGV-045: owner-derived asks follow the owner)
        for (Ask ask : askRepository.findByToAndStatusPending(oldOwner)) {
            ask.setTo(newOwner);
            askRepository.save(ask);
            auditService.logSystem("REKEY_ASK", "ask", ask.getId(),
                String.format("{\"oldDomain\":\"%s\",\"newDomain\":\"%s\",\"oldTo\":\"%s\",\"newTo\":\"%s\"}",
                    oldDomainId, newDomainId, oldOwner, newOwner));
        }
    }

    public List<DnaDomain> findByOwnerHumanId(String ownerId) {
        return domainRepository.findByOwnerHumanId(ownerId);
    }
}
