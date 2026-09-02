package com.summa.service;

import com.summa.enums.RbacRole;
import com.summa.repository.HumanRepository;
import com.summa.repository.AgentRepository;
import com.summa.model.Human;
import com.summa.model.Agent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;

@Service
public class MemberService {
    private final HumanRepository humanRepository;
    private final AgentRepository agentRepository;

    public MemberService(HumanRepository humanRepository, AgentRepository agentRepository) {
        this.humanRepository = humanRepository;
        this.agentRepository = agentRepository;
    }

    public Optional<Human> findHuman(String id) {
        return humanRepository.findById(id);
    }

    public Optional<Agent> findAgent(String id) {
        return agentRepository.findById(id);
    }

    public List<Human> findAllActiveHumans() {
        return humanRepository.findAllActive();
    }

    public List<Agent> findAllActiveAgents() {
        return agentRepository.findAllActive();
    }

    public List<Human> findAdmins() {
        return humanRepository.findActiveByRole(RbacRole.ADMIN.getValue());
    }

    public List<Human> findOwnerHumans() {
        return humanRepository.findActiveByRole(RbacRole.OWNER.getValue());
    }

    public long countActiveAdmins() {
        return humanRepository.countByDeactivatedAtIsNullAndRbac(RbacRole.ADMIN.getValue());
    }

    public boolean isViewer(Human human) {
        return human != null && RbacRole.VIEWER.getValue().equals(human.getRbac());
    }

    public boolean hasWriteSurface(Human human) {
        return human != null && !isViewer(human) && human.isActive();
    }

    public boolean hasWriteSurfaceAgent(Agent agent) {
        return agent != null && agent.isActive() && !agent.isEphemeral();
    }

    public boolean isAdmin(String actorId) {
        if (actorId == null || "system".equals(actorId)) return false;
        Optional<Human> humanOpt = findHuman(actorId);
        return humanOpt.isPresent() && RbacRole.ADMIN.getValue().equals(humanOpt.get().getRbac());
    }

    @Transactional
    public Human saveHuman(Human human) {
        return humanRepository.save(human);
    }
}
