package com.summa.service;

import com.summa.repository.HumanRepository;
import com.summa.repository.AuditEventRepository;
import com.summa.repository.AskRepository;
import com.summa.repository.BoardTaskRepository;
import com.summa.repository.DnaProposalRepository;
import com.summa.repository.MemoryItemRepository;
import com.summa.repository.PatRepository;
import com.summa.repository.DataHoldRepository;
import com.summa.model.Human;
import com.summa.model.AuditEvent;
import com.summa.model.Ask;
import com.summa.model.BoardTask;
import com.summa.model.DnaProposal;
import com.summa.model.MemoryItem;
import com.summa.model.Pat;
import com.summa.security.PasswordUtil;
import com.summa.security.PasswordValidator;
import com.summa.util.JsonHelpers;
import com.summa.constants.Defaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.summa.exception.EntityNotFoundException;
import com.summa.exception.ConflictException;
import com.summa.enums.RbacRole;

@Service
public class OrgService {
    private final HumanRepository humanRepository;
    private final AuditService auditService;
    private final AuditEventRepository auditEventRepository;
    private final OffboardingWalkService offboardingWalkService;
    private final PasswordUtil passwordUtil;
    private final MemberService memberService;
    private final AskRepository askRepository;
    private final BoardTaskRepository boardTaskRepository;
    private final DnaProposalRepository proposalRepository;
    private final MemoryItemRepository memoryItemRepository;
    private final PatRepository patRepository;
    private final DataHoldRepository dataHoldRepository;

    public OrgService(HumanRepository humanRepository, AuditService auditService,
                      AuditEventRepository auditEventRepository,
                      OffboardingWalkService offboardingWalkService,
                      PasswordUtil passwordUtil,
                      MemberService memberService,
                      AskRepository askRepository,
                      BoardTaskRepository boardTaskRepository,
                      DnaProposalRepository proposalRepository,
                      MemoryItemRepository memoryItemRepository,
                      PatRepository patRepository,
                      DataHoldRepository dataHoldRepository) {
        this.humanRepository = humanRepository;
        this.auditService = auditService;
        this.auditEventRepository = auditEventRepository;
        this.offboardingWalkService = offboardingWalkService;
        this.passwordUtil = passwordUtil;
        this.memberService = memberService;
        this.askRepository = askRepository;
        this.boardTaskRepository = boardTaskRepository;
        this.proposalRepository = proposalRepository;
        this.memoryItemRepository = memoryItemRepository;
        this.patRepository = patRepository;
        this.dataHoldRepository = dataHoldRepository;
    }

    private String findErasureCustodyAdmin() {
        return memberService.findAdmins().stream()
                .map(h -> "h:" + h.getId())
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public Human bootstrap(String name, String email, String rbac, String password) {
        // Check if any human exists
        long count = humanRepository.count();
        if (count > 0) {
            throw new ConflictException("Company already bootstrapped");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (email == null || email.isBlank()
                || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")) {
            throw new IllegalArgumentException("A valid email is required");
        }
        // First user owns the org: force admin regardless of client-supplied rbac.
        // Accepting an arbitrary rbac here could brick the org with a viewer-only user.
        String effectiveRbac = "admin";
        PasswordValidator.validate(password);

        Human human = new Human();
        human.setId(UUID.randomUUID().toString());
        human.setName(name);
        human.setEmail(email);
        human.setRbac(effectiveRbac);
        human.setPasswordHash(passwordUtil.hash(password));

        Human saved = humanRepository.save(human);
        auditService.logSystem("BOOTSTRAP", "human", saved.getId(),
            "{\"name\":" + JsonHelpers.jsonString(name) + ",\"rbac\":" + JsonHelpers.jsonString(effectiveRbac) + "}");
        return saved;
    }

    @Transactional
    public Human createHuman(String name, String email, String rbac, String password) {
        PasswordValidator.validate(password);

        // Validate rbac is one of the recognized roles; default to "member" if null
        String effectiveRbac = "member";
        if (rbac != null && !rbac.isBlank()) {
            if (RbacRole.fromValue(rbac) == null) {
                throw new IllegalArgumentException("Invalid rbac: " + rbac + ". Must be one of: admin, owner, member, viewer");
            }
            effectiveRbac = rbac;
        }

        Human human = new Human();
        human.setId(UUID.randomUUID().toString());
        human.setName(name);
        human.setEmail(email);
        human.setRbac(effectiveRbac);
        human.setPasswordHash(passwordUtil.hash(password));

        Human saved = humanRepository.save(human);
        auditService.log(Defaults.SYSTEM_ACTOR, "CREATE_HUMAN", "human", saved.getId(),
            String.format("{\"name\":%s,\"rbac\":%s}", JsonHelpers.jsonString(name), JsonHelpers.jsonString(effectiveRbac)));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Human> findHuman(String id) {
        return humanRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Human> findHumanByEmail(String email) {
        return humanRepository.findByEmail(email != null ? email.toLowerCase() : email);
    }

    /**
     * OFB-020: Read the target human row under a pessimistic lock to prevent
     * concurrent mutations from interleaving (e.g. double password change).
     */
    public Optional<Human> findHumanForUpdate(String id) {
        return humanRepository.findByIdForUpdate(id);
    }

    @Transactional(readOnly = true)
    public List<Human> findAllHumans() {
        return humanRepository.findAll();
    }

    @Transactional(readOnly = true)
    public boolean isInitialized() {
        return humanRepository.count() > 0;
    }

    @Transactional(readOnly = true)
    public List<Human> findAllActiveHumans() {
        return humanRepository.findAllActive();
    }

    @Transactional
    public Human offboard(String id, String actor) {
        // OFB-020: Acquire the pessimistic lock on the target row first to serialize
        // concurrent offboard attempts on the same human.
        Human human = humanRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Human not found: " + id));

        // Then check last-admin guard with the lock held — no concurrent transaction
        // can offboard another admin until this transaction commits.
        long activeAdminCount = humanRepository.countByDeactivatedAtIsNullAndRbac("admin");
        if (activeAdminCount <= 1) {
            throw new IllegalStateException("Cannot offboard the last admin");
        }

        // Run the full dependency walk per OFB-001
        Map<String, Object> result = offboardingWalkService.walkOffboard(id, null, actor);

        auditService.log(actor, "OFFBOARD", "human", id,
            String.format("{\"result\":%s}", result));
        return human;
    }

    @Transactional
    public Human updateRbac(String id, String newRbac, String actor) {
        if (newRbac == null || newRbac.isBlank()) {
            throw new IllegalArgumentException("New RBAC role is required");
        }

        // Acquire the row lock first to serialize concurrent rbac updates
        Human human = humanRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Human not found: " + id));

        // OFB-021: Last-admin guard — same check as demote to prevent bricking the org
        long activeAdminCount = humanRepository.countByDeactivatedAtIsNullAndRbac("admin");
        boolean isCurrentAdmin = RbacRole.ADMIN.getValue().equals(human.getRbac());
        boolean becomesNonAdmin = isCurrentAdmin && !RbacRole.ADMIN.getValue().equals(newRbac);
        if (becomesNonAdmin && activeAdminCount <= 1) {
            throw new IllegalStateException("Cannot update rbac: would leave the org with zero admins");
        }

        // OFB-030: Run demotion walk when the new role strips write surfaces
        // (viewer cannot own domains, agents, initiatives, goals, proposals, or groups)
        boolean stripsWriteSurface = !memberService.canWrite(newRbac) && memberService.canWrite(human.getRbac());
        if (stripsWriteSurface) {
            offboardingWalkService.walkDemote(id, newRbac, actor);
        }

        human.setRbac(newRbac);
        Human saved = humanRepository.save(human);
        auditService.log(actor, "UPDATE_RBAC", "human", id,
            String.format("{\"newRbac\":%s}", JsonHelpers.jsonString(newRbac)));
        return saved;
    }

    @Transactional
    public Human setDeputy(String id, String deputyId, String actor) {
        // Pessimistic lock to prevent concurrent deputy assignment interleaving
        Human human = humanRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Human not found: " + id));

        // Validate deputy exists and is not a viewer
        Optional<Human> deputyOpt = humanRepository.findById(deputyId);
        if (deputyOpt.isEmpty()) {
            throw new EntityNotFoundException("Deputy not found: " + deputyId);
        }
        Human deputy = deputyOpt.get();
        if (RbacRole.VIEWER.getValue().equals(deputy.getRbac())) {
            throw new IllegalArgumentException("Deputy cannot be a viewer");
        }
        if (deputy.getId().equals(id)) {
            throw new IllegalArgumentException("Cannot deputy self");
        }

        // ORG-061: Detect deputy cycles — refuse if setting this deputy would create a cycle
        if (wouldCreateDeputyCycle(id, deputyId)) {
            throw new IllegalStateException("Cannot set deputy: would create a cycle in deputy chain");
        }

        human.setDeputyMemberId(deputyId);
        Human saved = humanRepository.save(human);
        auditService.log(actor, "SET_DEPUTY", "human", id,
            String.format("{\"deputyId\":%s}", JsonHelpers.jsonString(deputyId)));
        return saved;
    }

    /**
     * ORG-061: Check if setting deputyId as deputy of humanId would create a cycle.
     * Walks the deputy chain from deputyId to see if it ever reaches humanId.
     */
    private boolean wouldCreateDeputyCycle(String humanId, String deputyId) {
        String current = deputyId;
        int maxSteps = Defaults.DEFAULT_CYCLE_DETECTION_MAX_STEPS;
        java.util.Set<String> visited = new java.util.HashSet<>();
        for (int i = 0; i < maxSteps; i++) {
            Optional<Human> h = humanRepository.findById(current);
            if (h.isEmpty()) return false;
            String deputy = h.get().getDeputyMemberId();
            if (deputy == null) return false;
            if (deputy.equals(humanId)) return true;
            if (!visited.add(current)) return true; // pre-existing cycle
            current = deputy;
        }
        return true; // Exceeded max steps — treat as cycle
    }

    @Transactional
    public Human demote(String id, String newRbac, String actor) {
        // Pessimistic lock to prevent concurrent demotion/offboard interleaving
        Human human = humanRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Human not found: " + id));

        // OFB-021: Last-admin guard — demotion joining deactivation under the same transactional check
        long activeAdminCount = humanRepository.countByDeactivatedAtIsNullAndRbac("admin");
        boolean isCurrentAdmin = RbacRole.ADMIN.getValue().equals(human.getRbac());
        boolean becomesNonAdmin = isCurrentAdmin && !RbacRole.ADMIN.getValue().equals(newRbac);
        if (becomesNonAdmin && activeAdminCount <= 1) {
            throw new IllegalStateException("Cannot demote the last admin");
        }

        // OFB-030: Run the demotion walk scoped to what the new role can no longer carry
        offboardingWalkService.walkDemote(id, newRbac, actor);

        auditService.log(actor, "DEMOTE", "human", id,
            String.format("{\"newRbac\":%s}", JsonHelpers.jsonString(newRbac)));
        return human;
    }

    @Transactional
    public Human saveHuman(Human human) {
        return humanRepository.save(human);
    }

    public List<AuditEvent> getAuditLog(int limit) {
        return auditEventRepository.findRecent(limit);
    }

    public List<AuditEvent> getAuditLogForEntity(String objectType, String objectId) {
        return auditEventRepository.findByObject(objectType, objectId);
    }

    @Transactional
    public void erasure(String id, String actor) {
        // Pessimistic lock to prevent concurrent erasure/offboard interleaving
        Human human = humanRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Human not found: " + id));

        // Anonymize identity fields per STG-030..034
        human.setName("[ERASED]");
        human.setEmail("[ERASED]");
        human.setPasswordHash(null);
        human.setDeputyMemberId(null);
        human.setDeactivatedAt(Instant.now());
        humanRepository.save(human);

        String cleanId = JsonHelpers.stripIdPrefix(id);
        // STG-034: legal holds (kind member) freeze erasure inside the transaction
        // so a hold created between any external check and the sweep cannot slip through
        if (!dataHoldRepository.findByKindAndSubjectIdAndReleasedAtIsNull("member", cleanId).isEmpty()) {
            throw new ConflictException("Active data holds prevent erasure");
        }

        // STG-031: Sweep DNA proposal attribution to the erasure custody admin
        String erasureCustody = findErasureCustodyAdmin();
        if (erasureCustody == null) {
            // No live admin can take custody: erasure would strand keyed-union rows — refuse
            throw new ConflictException("No active admin to assume custody; erasure refused");
        }
        for (DnaProposal prop : proposalRepository.findByProposedBy(cleanId)) {
            prop.setProposedBy(erasureCustody);
            proposalRepository.save(prop);
        }
        for (DnaProposal prop : proposalRepository.findByProposedBy("h:" + cleanId)) {
            prop.setProposedBy(erasureCustody);
            proposalRepository.save(prop);
        }

        // STG-031: Sweep ask attribution. asks.to takes custody of the departed member's
        // pending addressee role; asks.from is re-pointed to the system originator —
        // 'admins' is legal only on asks.to and the CHECK constraint rejects it on from.
        for (Ask ask : askRepository.findByFrom(cleanId)) {
            ask.setFrom("system");
            askRepository.save(ask);
        }
        for (Ask ask : askRepository.findByFrom("h:" + cleanId)) {
            ask.setFrom("system");
            askRepository.save(ask);
        }
        for (Ask ask : askRepository.findByTo(cleanId)) {
            ask.setTo(erasureCustody);
            askRepository.save(ask);
        }
        for (Ask ask : askRepository.findByTo("h:" + cleanId)) {
            ask.setTo(erasureCustody);
            askRepository.save(ask);
        }

        // STG-031: Completed board-task assignments keep the event shape with the
        // member link severed (null); a live assignee cannot be an erased member.
        for (BoardTask task : boardTaskRepository.findByAssigneeMemberId(cleanId)) {
            if (cleanId.equals(task.getAssigneeMemberId())) {
                task.setAssigneeMemberId(null);
                boardTaskRepository.save(task);
            }
        }
        for (BoardTask task : boardTaskRepository.findByAssigneeMemberId("h:" + cleanId)) {
            task.setAssigneeMemberId(null);
            boardTaskRepository.save(task);
        }

        // STG-034: Sweep memory item attribution. member_id is an FK to agents(id),
        // so a pseudonymized human ref nulls out (severed) rather than taking custody.
        for (MemoryItem mem : memoryItemRepository.findByMemberId(cleanId)) {
            mem.setMemberId(null);
            memoryItemRepository.save(mem);
        }
        for (MemoryItem mem : memoryItemRepository.findByMemberId("h:" + cleanId)) {
            mem.setMemberId(null);
            memoryItemRepository.save(mem);
        }

        // STG-031: Revoke the member's personal access tokens (DAT-120: sweep both forms)
        for (String memberRef : List.of(cleanId, "h:" + cleanId)) {
            for (Pat pat : patRepository.findByMemberId(memberRef)) {
                pat.setRevokedAt(Instant.now());
                patRepository.save(pat);
            }
        }

        // STG-030/034: legal holds (kind member) freeze erasure inside the transaction
        // so a hold created between the controller check and the sweep cannot slip through
        if (!dataHoldRepository.findByKindAndSubjectIdAndReleasedAtIsNull("member", cleanId).isEmpty()) {
            throw new ConflictException("Active data holds prevent erasure");
        }

        auditService.log(actor, "ERASURE", "human", id, null);
    }
}
