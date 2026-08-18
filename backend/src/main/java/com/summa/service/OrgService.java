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

        Human human = new Human();
        human.setId(UUID.randomUUID().toString());
        human.setName(name);
        human.setEmail(email);
        human.setRbac(rbac != null ? rbac : "admin");
        human.setAuth("{}");
        human.setPasswordHash(password != null && !password.isBlank() ? passwordUtil.hash(password) : null);

        Human saved = humanRepository.save(human);
        auditService.log("system", "BOOTSTRAP", "human", saved.getId(),
            String.format("{\"name\":\"%s\",\"rbac\":\"%s\"}", name, rbac));
        return saved;
    }

    @Transactional
    public Human createHuman(String name, String email, String rbac, String auth, String password) {
        Human human = new Human();
        human.setId(UUID.randomUUID().toString());
        human.setName(name);
        human.setEmail(email);
        human.setRbac(rbac != null ? rbac : "member");
        human.setAuth(auth != null ? auth : "{}");
        human.setPasswordHash(password != null && !password.isBlank() ? passwordUtil.hash(password) : null);

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
        Human human = humanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Human not found: " + id));

        // Check last admin guard
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

        human.setDeputyMemberId(deputyId);
        Human saved = humanRepository.save(human);
        auditService.log(actor, "SET_DEPUTY", "human", id,
            String.format("{\"deputyId\":\"%s\"}", deputyId));
        return saved;
    }

    public Human saveHuman(Human human) {
        return humanRepository.save(human);
    }

    public List<AuditEvent> getAuditLog(int limit) {
        return auditEventRepository.findRecent(limit);
    }

    public List<AuditEvent> getAuditLogForEntity(String objectType, String objectId) {
        return auditEventRepository.findByObject(objectType, objectId);
    }
}
