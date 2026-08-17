package com.summa.service;

import com.summa.repository.HumanRepository;
import com.summa.repository.AuditEventRepository;
import com.summa.model.Human;
import com.summa.model.AuditEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrgService {
    private final HumanRepository humanRepository;
    private final AuditService auditService;
    private final AuditEventRepository auditEventRepository;

    public OrgService(HumanRepository humanRepository, AuditService auditService,
                      AuditEventRepository auditEventRepository) {
        this.humanRepository = humanRepository;
        this.auditService = auditService;
        this.auditEventRepository = auditEventRepository;
    }

    public Human bootstrap(String name, String email, String rbac) {
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

        Human saved = humanRepository.save(human);
        auditService.log("system", "BOOTSTRAP", "human", saved.getId(),
            String.format("{\"name\":\"%s\",\"rbac\":\"%s\"}", name, rbac));
        return saved;
    }

    public Human createHuman(String name, String email, String rbac, String auth) {
        Human human = new Human();
        human.setId(UUID.randomUUID().toString());
        human.setName(name);
        human.setEmail(email);
        human.setRbac(rbac != null ? rbac : "member");
        human.setAuth(auth != null ? auth : "{}");

        Human saved = humanRepository.save(human);
        auditService.log("system", "CREATE_HUMAN", "human", saved.getId(),
            String.format("{\"name\":\"%s\",\"rbac\":\"%s\"}", name, rbac));
        return saved;
    }

    public Optional<Human> findHuman(String id) {
        return humanRepository.findById(id);
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
        long adminCount = humanRepository.countByDeactivatedAtIsNull();
        if (adminCount <= 1 && "admin".equals(human.getRbac())) {
            throw new IllegalStateException("Cannot offboard the last admin");
        }

        human.setDeactivatedAt(Instant.now());
        Human saved = humanRepository.save(human);
        auditService.log(actor, "OFFBOARD", "human", id, null);
        return saved;
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
        humanRepository.findById(deputyId).ifPresent(deputy -> {
            if ("viewer".equals(deputy.getRbac())) {
                throw new IllegalArgumentException("Deputy cannot be a viewer");
            }
            if (deputy.getId().equals(id)) {
                throw new IllegalArgumentException("Cannot deputy self");
            }
        });

        human.setDeputyMemberId(deputyId);
        Human saved = humanRepository.save(human);
        auditService.log(actor, "SET_DEPUTY", "human", id,
            String.format("{\"deputyId\":\"%s\"}", deputyId));
        return saved;
    }

    public List<AuditEvent> getAuditLog(int limit) {
        return auditEventRepository.findRecent(limit);
    }

    public List<AuditEvent> getAuditLogForEntity(String objectType, String objectId) {
        return auditEventRepository.findByObject(objectType, objectId);
    }
}
