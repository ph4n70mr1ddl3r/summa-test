package com.summa.service;

import com.summa.enums.RbacRole;
import com.summa.constants.Defaults;
import com.summa.repository.HumanRepository;
import com.summa.repository.AgentRepository;
import com.summa.model.Human;
import com.summa.model.Agent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.summa.util.JsonHelpers;
import com.summa.util.KeyedUnionValidator;

@Service
public class MemberService {
    private final HumanRepository humanRepository;
    private final AgentRepository agentRepository;

    public MemberService(HumanRepository humanRepository, AgentRepository agentRepository) {
        this.humanRepository = humanRepository;
        this.agentRepository = agentRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Human> findHuman(String id) {
        return humanRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Agent> findAgent(String id) {
        return agentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Human> findAllActiveHumans() {
        return humanRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public List<Agent> findAllActiveAgents() {
        return agentRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public List<Human> findAdmins() {
        return humanRepository.findActiveByRole(RbacRole.ADMIN.getValue());
    }

    @Transactional(readOnly = true)
    public long countActiveAdmins() {
        return humanRepository.countByDeactivatedAtIsNullAndRbac(RbacRole.ADMIN.getValue());
    }

    public boolean isViewer(Human human) {
        return human != null && RbacRole.VIEWER.getValue().equals(human.getRbac());
    }

    public boolean hasWriteSurface(Human human) {
        if (human == null) return false;
        return human.isActive() && !isViewer(human);
    }

    public boolean hasWriteSurfaceAgent(Agent agent) {
        return agent != null && agent.isActive() && !agent.isEphemeral();
    }

    /**
     * Admin authority is evaluated against live state: a deactivated human is no
     * longer an admin even if its row still carries the role (OFB-015 — deactivation
     * is credential-death, and the role lapses with the membership).
     */
    public boolean isAdmin(String actorId) {
        if (actorId == null || Defaults.SYSTEM_ACTOR.equals(actorId)) return false;
        String cleanId = JsonHelpers.stripIdPrefix(actorId);
        Optional<Human> humanOpt = findHuman(cleanId);
        return humanOpt.isPresent()
                && humanOpt.get().isActive()
                && RbacRole.ADMIN.getValue().equals(humanOpt.get().getRbac());
    }

    /**
     * DAT-120: resolve a caller-supplied member reference to the stored keyed-union
     * form — {@code h:<humans.id>} or {@code a:<agents.id>}. The kind is resolved by
     * live lookup, so an agent reference is never relabelled as a human one (a
     * mislabelled kind makes the row resolve against the wrong live table).
     *
     * <p>The reserved spellings {@code system} and {@code admins} pass through
     * unchanged; a reference to no live member resolves to {@code null} so callers
     * can refuse the write rather than store a dangling id.
     */
    @Transactional(readOnly = true)
    public String toKeyedMemberRef(String ref) {
        if (ref == null || ref.isBlank()) return null;
        String clean = JsonHelpers.stripIdPrefix(ref);
        if (clean == null) return null;
        if (KeyedUnionValidator.RESERVED_IDS.contains(clean)) return clean;
        if (findHuman(clean).isPresent()) return "h:" + clean;
        if (findAgent(clean).isPresent()) return "a:" + clean;
        return null;
    }

    public boolean canWrite(String rbac) {
        if (rbac == null) return false;
        try {
            return RbacRole.valueOf(rbac.toUpperCase()).canWrite();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Resolve the effective successor for delegation: deputy if eligible, otherwise returns null.
     * A deputy is eligible when active and not a viewer.
     * Returns the deputy id in the keyed-union representation used by asks.to /
     * initiatives.sponsor / initiatives.lead (h:<id>).
     */
    @Transactional(readOnly = true)
    public Optional<String> resolveDeputyTarget(String memberId) {
        if (memberId == null || memberId.isBlank()) return Optional.empty();
        String cleanId = JsonHelpers.stripIdPrefix(memberId);
        Optional<Human> humanOpt = findHuman(cleanId);
        if (!humanOpt.isPresent() || !humanOpt.get().isActive() || humanOpt.get().getDeputyMemberId() == null) {
            return Optional.empty();
        }
        String deputyId = humanOpt.get().getDeputyMemberId();
        Optional<Human> deputyOpt = findHuman(JsonHelpers.stripIdPrefix(deputyId));
        if (deputyOpt.isPresent() && deputyOpt.get().isActive() && !"viewer".equals(deputyOpt.get().getRbac())) {
            return Optional.of("h:" + deputyOpt.get().getId());
        }
        return Optional.empty();
    }

    @Transactional
    public Human saveHuman(Human human) {
        return humanRepository.save(human);
    }

    /**
     * Walk the lineage chain from a member ID, following agent spawned-by links
     * and human deputy links, up to the configured depth cap. Returns the chain
     * of member IDs from the starting point to the root. Stops on cycles.
     */
    @Transactional(readOnly = true)
    public List<String> walkLineage(String memberId, int depthCap) {
        List<String> lineage = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String currentId = JsonHelpers.stripIdPrefix(memberId);
        while (currentId != null && lineage.size() < depthCap) {
            if (!visited.add(currentId)) {
                break;
            }
            lineage.add(currentId);
            Optional<Agent> agentOpt = findAgent(currentId);
            if (agentOpt.isPresent()) {
                currentId = agentOpt.get().getSpawnedBy();
            } else {
                Optional<Human> humanOpt = findHuman(currentId);
                if (humanOpt.isPresent()) {
                    currentId = humanOpt.get().getDeputyMemberId();
                } else {
                    currentId = null;
                }
            }
        }
        return lineage;
    }
}
