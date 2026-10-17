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

    public boolean isAdmin(String actorId) {
        if (actorId == null || Defaults.SYSTEM_ACTOR.equals(actorId)) return false;
        String cleanId = JsonHelpers.stripIdPrefix(actorId);
        Optional<Human> humanOpt = findHuman(cleanId);
        return humanOpt.isPresent() && RbacRole.ADMIN.getValue().equals(humanOpt.get().getRbac());
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
        Optional<Human> deputyOpt = findHuman(deputyId);
        if (deputyOpt.isPresent() && deputyOpt.get().isActive() && !"viewer".equals(deputyOpt.get().getRbac())) {
            return Optional.of(deputyOpt.get().getId());
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
