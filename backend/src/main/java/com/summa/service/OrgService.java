package com.summa.service;

import com.summa.repository.HumanRepository;
import com.summa.repository.AuditEventRepository;
import com.summa.model.Human;
import com.summa.model.AuditEvent;
import com.summa.security.PasswordUtil;
import com.summa.security.PasswordValidator;
import com.summa.util.JsonHelpers;
import com.summa.constants.Defaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import com.summa.exception.EntityNotFoundException;

@Service
public class OrgService {
    private final HumanRepository humanRepository;
    private final AuditService auditService;
    private final AuditEventRepository auditEventRepository;
    private final OffboardingWalkService offboardingWalkService;
    private final PasswordUtil passwordUtil;

    public OrgService(HumanRepository humanRepository, AuditService auditService,
                      AuditEventRepository auditEventRepository,
                      OffboardingWalkService offboardingWalkService,
                      PasswordUtil passwordUtil) {
        this.humanRepository = humanRepository;
        this.auditService = auditService;
        this.auditEventRepository = auditEventRepository;
        this.offboardingWalkService = offboardingWalkService;
        this.passwordUtil = passwordUtil;
    }

    @Transactional
    public Human bootstrap(String name, String email, String rbac, String password) {
        // Check if any human exists
        long count = humanRepository.count();
        if (count > 0) {
            throw new IllegalStateException("Company already bootstrapped");
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
        human.setAuth("{}");
        human.setPasswordHash(passwordUtil.hash(password));

        Human saved = humanRepository.save(human);
        auditService.logSystem("BOOTSTRAP", "human", saved.getId(),
            "{\"name\":" + JsonHelpers.jsonString(name) + ",\"rbac\":" + JsonHelpers.jsonString(effectiveRbac) + "}");
        return saved;
    }

    @Transactional
    public Human createHuman(String name, String email, String rbac, String auth, String password) {
        PasswordValidator.validate(password);

        Human human = new Human();
        human.setId(UUID.randomUUID().toString());
        human.setName(name);
        human.setEmail(email);
        human.setRbac(rbac != null ? rbac : "member");
        human.setAuth(auth != null ? auth : "{}");
        human.setPasswordHash(passwordUtil.hash(password));

        Human saved = humanRepository.save(human);
        auditService.log(Defaults.SYSTEM_ACTOR, "CREATE_HUMAN", "human", saved.getId(),
            String.format("{\"name\":%s,\"rbac\":%s}", JsonHelpers.jsonString(name), JsonHelpers.jsonString(rbac)));
        return saved;
    }

    public Optional<Human> findHuman(String id) {
        return humanRepository.findById(id);
    }

    public Optional<Human> findHumanByEmail(String email) {
        return humanRepository.findByEmail(email);
    }

    /**
     * OFB-020: Read the target human row under a pessimistic lock to prevent
     * concurrent mutations from interleaving (e.g. double password change).
     */
    public Optional<Human> findHumanForUpdate(String id) {
        return humanRepository.findByIdForUpdate(id);
    }

    public List<Human> findAllHumans() {
        return humanRepository.findAll();
    }

    public boolean isInitialized() {
        return humanRepository.count() > 0;
    }

    public List<Human> findAllActiveHumans() {
        return humanRepository.findAllActive();
    }

    @Transactional
    public Human offboard(String id, String actor) {
        // OFB-020: Check last-admin guard BEFORE acquiring the row lock to prevent
        // a concurrent offboard of another admin from slipping between our count
        // check and our deactivate, which would leave zero live admins.
        long activeAdminCount = humanRepository.countByDeactivatedAtIsNullAndRbac("admin");
        if (activeAdminCount <= 1) {
            throw new IllegalStateException("Cannot offboard the last admin");
        }

        // Then acquire the pessimistic lock on the target row to prevent concurrent
        // mutations of the same human (e.g. double password change).
        Human human = humanRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Human not found: " + id));

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

        // OFB-021: Last-admin guard — same check as demote to prevent bricking the org
        long activeAdminCount = humanRepository.countByDeactivatedAtIsNullAndRbac("admin");
        boolean isCurrentAdmin = humanRepository.findById(id).map(h -> "admin".equals(h.getRbac())).orElse(false);
        boolean becomesNonAdmin = isCurrentAdmin && !"admin".equals(newRbac);
        if (becomesNonAdmin && activeAdminCount <= 1) {
            throw new IllegalStateException("Cannot update rbac: would leave the org with zero admins");
        }

        Human human = humanRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Human not found: " + id));

        human.setRbac(newRbac);
        Human saved = humanRepository.save(human);
        auditService.log(actor, "UPDATE_RBAC", "human", id,
            String.format("{\"newRbac\":%s}", JsonHelpers.jsonString(newRbac)));
        return saved;
    }

    @Transactional
    public Human setDeputy(String id, String deputyId, String actor) {
        Human human = humanRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Human not found: " + id));

        // Validate deputy exists and is not a viewer
        Optional<Human> deputyOpt = humanRepository.findById(deputyId);
        if (deputyOpt.isEmpty()) {
            throw new EntityNotFoundException("Deputy not found: " + deputyId);
        }
        Human deputy = deputyOpt.get();
        if ("viewer".equals(deputy.getRbac())) {
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
        for (int i = 0; i < maxSteps; i++) {
            Optional<Human> h = humanRepository.findById(current);
            if (h.isEmpty()) return false;
            String deputy = h.get().getDeputyMemberId();
            if (deputy == null) return false;
            if (deputy.equals(humanId)) return true;
            current = deputy;
        }
        return true; // Exceeded max steps — treat as cycle
    }

    @Transactional
    public Human demote(String id, String newRbac, String actor) {
        Human human = humanRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Human not found: " + id));

        // OFB-021: Last-admin guard — demotion joining deactivation under the same transactional check
        long activeAdminCount = humanRepository.countByDeactivatedAtIsNullAndRbac("admin");
        boolean isCurrentAdmin = "admin".equals(human.getRbac());
        boolean becomesNonAdmin = "admin".equals(human.getRbac()) && !"admin".equals(newRbac);
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
        Human human = humanRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Human not found: " + id));

        // Anonymize identity fields per STG-030..034
        human.setName("[ERASED]");
        human.setEmail("[ERASED]");
        human.setPasswordHash(null);
        human.setAuth("{}");
        human.setDeputyMemberId(null);
        human.setDeactivatedAt(Instant.now());
        humanRepository.save(human);

        auditService.log(actor, "ERASURE", "human", id, null);
    }
}
