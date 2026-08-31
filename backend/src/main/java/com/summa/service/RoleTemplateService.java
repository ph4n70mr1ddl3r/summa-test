package com.summa.service;

import com.summa.repository.RoleTemplateRepository;
import com.summa.model.RoleTemplate;
import com.summa.repository.AgentRepository;
import com.summa.model.Agent;
import com.summa.repository.SpawnRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RoleTemplateService {
    private final RoleTemplateRepository templateRepository;
    private final AuditService auditService;
    private final AgentRepository agentRepository;
    private final SpawnRequestRepository spawnRequestRepository;
    private final AskService askService;

    public RoleTemplateService(RoleTemplateRepository templateRepository, AuditService auditService,
                               AgentRepository agentRepository, SpawnRequestRepository spawnRequestRepository,
                               AskService askService) {
        this.templateRepository = templateRepository;
        this.auditService = auditService;
        this.agentRepository = agentRepository;
        this.spawnRequestRepository = spawnRequestRepository;
        this.askService = askService;
    }

    @Transactional
    public RoleTemplate create(String name, String agentClass, String body, String defaultScopes) {
        RoleTemplate template = new RoleTemplate();
        template.setId(UUID.randomUUID().toString());
        template.setName(name);
        template.setAgentClass(agentClass);
        template.setBody(body != null ? body : "{}");
        template.setDefaultScopes(defaultScopes != null ? defaultScopes : "{}");
        template.setStatus("draft");

        RoleTemplate saved = templateRepository.save(template);
        auditService.log("system", "CREATE", "role_template", template.getId(),
            String.format("{\"name\":\"%s\",\"class\":\"%s\"}", name, agentClass));
        return saved;
    }

    public Optional<RoleTemplate> findById(String id) {
        return templateRepository.findById(id);
    }

    public List<RoleTemplate> findAll() {
        return templateRepository.findAll();
    }

    /**
     * TPL-010/011: Publish a new active version and file upgrade asks to each pinned agent's owner.
     * Publication supersedes but never retires — a denied or expired upgrade leaves the pin
     * on its still-legitimate active row, and the next bump re-asks.
     */
    @Transactional
    public RoleTemplate publish(String id, String actor) {
        RoleTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));

        // TPL-003: Class is immutable across a name's versions — check against any existing row
        List<RoleTemplate> sameName = templateRepository.findByClassAndName(template.getAgentClass(), template.getName());
        for (RoleTemplate existing : sameName) {
            if (!existing.getId().equals(id) && !existing.getAgentClass().equals(template.getAgentClass())) {
                throw new IllegalStateException(
                    "Class flip refused: name '" + template.getName() + "' already has class '" + existing.getAgentClass() + "'");
            }
        }

        // Bump version for the new active row
        Integer nextVersion = template.getVersion() != null ? template.getVersion() + 1 : 2;
        template.setVersion(nextVersion);
        template.setStatus("active");
        RoleTemplate saved = templateRepository.save(template);

        // TPL-011: File upgrade ask to each pinned agent's owner
        List<Agent> pinnedAgents = agentRepository.findAll().stream()
                .filter(a -> id.equals(a.getTemplateId()) && a.isActive())
                .toList();
        for (Agent agent : pinnedAgents) {
            try {
                String payload = String.format(
                    "{\"templateId\":\"%s\",\"newVersion\":%d,\"agentId\":\"%s\",\"agentName\":\"%s\"}",
                    id, nextVersion, agent.getId(), agent.getName());
                askService.create("upgrade", agent.getOwnerHumanId(), "admins",
                    payload, "standard", "deny", 1,
                    Instant.now().plusSeconds(7 * 86400L), null, null);
                auditService.logSystem("UPGRADE_ASK_FILED", "role_template", id,
                    String.format("{\"agentId\":\"%s\",\"owner\":\"%s\",\"version\":%d}",
                        agent.getId(), agent.getOwnerHumanId(), nextVersion));
            } catch (Exception e) {
                auditService.logSystem("UPGRADE_ASK_FAIL", "role_template", id,
                    String.format("{\"agentId\":\"%s\",\"error\":\"%s\"}", agent.getId(), e.getMessage()));
            }
        }

        auditService.log(actor, "PUBLISH", "role_template", id,
            String.format("{\"version\":%d}", nextVersion));
        return saved;
    }

    /**
     * TPL-030: Retiring a template with live pins is refused.
     * Pins count pending spawn requests as well as running agents.
     */
    @Transactional
    public RoleTemplate retire(String id, String actor) {
        RoleTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));

        // Check for live pins: active agents pinned to this template
        long activeAgentPins = agentRepository.findAll().stream()
                .filter(a -> id.equals(a.getTemplateId()) && a.isActive())
                .count();
        // Check for pending spawn requests pinning this template
        long pendingSpawnPins = spawnRequestRepository.findByStatus("requested").stream()
                .filter(s -> id.equals(s.getTemplateId()))
                .count();

        if (activeAgentPins > 0 || pendingSpawnPins > 0) {
            throw new IllegalStateException(
                String.format("Cannot retire template with live pins: %d active agents, %d pending spawns",
                    activeAgentPins, pendingSpawnPins));
        }

        template.setStatus("retired");
        RoleTemplate saved = templateRepository.save(template);
        auditService.log(actor, "RETIRE", "role_template", id, null);
        return saved;
    }
}

