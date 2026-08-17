package com.summa.service;

import com.summa.repository.HumanRepository;
import com.summa.repository.AgentRepository;
import com.summa.model.Human;
import com.summa.model.Agent;
import org.springframework.stereotype.Service;
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
        return humanRepository.findActiveByRole("admin");
    }

    public List<Human> findOwnerHumans() {
        return humanRepository.findActiveByRole("owner");
    }

    public long countActiveAdmins() {
        return humanRepository.countByDeactivatedAtIsNull();
    }

    public boolean isViewer(Human human) {
        return "viewer".equals(human.getRbac());
    }

    public boolean hasWriteSurface(Human human) {
        return human != null && !"viewer".equals(human.getRbac()) && human.isActive();
    }

    public boolean hasWriteSurfaceAgent(Agent agent) {
        return agent != null && agent.isActive() && !"ephemeral".equals(agent.getAgentClass());
    }

    public Human saveHuman(Human human) {
        return humanRepository.save(human);
    }
}
