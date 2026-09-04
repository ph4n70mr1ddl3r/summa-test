package com.summa.service;

import com.summa.repository.HumanRepository;
import com.summa.repository.AuditEventRepository;
import com.summa.model.Human;
import com.summa.model.AuditEvent;
import com.summa.security.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;

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
                || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("A valid email is required");
        }
        // First user owns the org: force admin regardless of client-supplied rbac.
        // Accepting an arbitrary rbac here could brick the org with a viewer-only user.
        String effectiveRbac = "admin";
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }

        Human human = new Human();
        human.setId(UUID.randomUUID().toString());
        human.setName(name);
        human.setEmail(email);
        human.setRbac(effectiveRbac);
        human.setAuth("{}");
        human.setPasswordHash(passwordUtil.hash(password));

        Human saved = humanRepository.save(human);
        auditService.log("system", "BOOTSTRAP", "human", saved.getId(),
            "{\"name\":" + jsonString(name) + ",\"rbac\":" + jsonString(effectiveRbac) + "}");
        return saved;
    }

    /**
     * Minimal JSON string escaper for audit payloads (avoids pulling
     * ObjectMapper into this service and prevents log/JSON injection).
     */
    static String jsonString(String value) {
        if (value == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append("\"").toString();
    }

    @Transactional
    public Human createHuman(String name, String email, String rbac, String auth, String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }

        Human human = new Human();
        human.setId(UUID.randomUUID().toString());
        human.setName(name);
        human.setEmail(email);
        human.setRbac(rbac != null ? rbac : "member");
        human.setAuth(auth != null ? auth : "{}");
        human.setPasswordHash(passwordUtil.hash(password));

        Human saved = humanRepository.save(human);
        auditService.log("system", "CREATE_HUMAN", "human", saved.getId(),
            String.format("{\"name\":\"%s\",\"rbac\":\"%s\"}", name, rbac));
        return saved;
    }

    public Optional<Human> findHuman(String id) {
        return humanRepository.findById(id);
    }

    public Optional<Human> findHumanByEmail(String email) {
        return humanRepository.findByEmail(email);
    }

    public List<Human> findAllHumans() {
        return humanRepository.findAll();
    }

    public List<Human> findAllActiveHumans() {
        return humanRepository.findAllActive();
    }

    @Transactional
    public Human offboard(String id, String actor) {
        // OFB-020: Use pessimistic write lock on the target row to prevent a
        // concurrent offboard of another admin from slipping between our
        // admin-count check and our deactivate, which would leave zero live admins.
        Human human = humanRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Human not found: " + id));

        // Check last admin guard while holding the row lock
        long activeAdminCount = humanRepository.countByDeactivatedAtIsNullAndRbac("admin");
        if (activeAdminCount <= 1 && "admin".equals(human.getRbac())) {
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
        Human human = humanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Human not found: " + id));

        human.setRbac(newRbac);
        Human saved = humanRepository.save(human);
        auditService.log(actor, "UPDATE_RBAC", "human", id,
            String.format("{\"newRbac\":\"%s\"}", newRbac));
        return saved;
    }

    @Transactional
    public Human setDeputy(String id, String deputyId, String actor) {
        Human human = humanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Human not found: " + id));

        // Validate deputy exists and is not a viewer
        Optional<Human> deputyOpt = humanRepository.findById(deputyId);
        if (deputyOpt.isEmpty()) {
            throw new IllegalArgumentException("Deputy not found: " + deputyId);
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
            String.format("{\"deputyId\":\"%s\"}", deputyId));
        return saved;
    }

    /**
     * ORG-061: Check if setting deputyId as deputy of humanId would create a cycle.
     * Walks the deputy chain from deputyId to see if it ever reaches humanId.
     */
    private boolean wouldCreateDeputyCycle(String humanId, String deputyId) {
        String current = deputyId;
        int maxSteps = 50; // Safety bound
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
                .orElseThrow(() -> new IllegalArgumentException("Human not found: " + id));

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
            String.format("{\"newRbac\":\"%s\"}", newRbac));
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
                .orElseThrow(() -> new IllegalArgumentException("Human not found: " + id));

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
