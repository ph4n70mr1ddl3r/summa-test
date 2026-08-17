package com.summa.service;

import com.summa.repository.*;
import com.summa.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class OffboardingWalkService {
    private final MemberService memberService;
    private final AgentService agentService;
    private final InitiativeService initiativeService;
    private final BoardTaskService boardTaskService;
    private final DnaProposalService proposalService;
    private final AuditService auditService;
    private final AskService askService;
    private final SpawnService spawnService;
    private final DnaDomainService domainService;
    private final DnaGoalService goalService;
    private final GroupMembershipRepository groupMembershipRepository;
    private final AgentRepository agentRepository;
    private final InitiativeRepository initiativeRepository;
    private final DnaGoalRepository goalRepository;
    private final DnaProposalRepository proposalRepository;

    public OffboardingWalkService(MemberService memberService, AgentService agentService,
                                    InitiativeService initiativeService, BoardTaskService boardTaskService,
                                    DnaProposalService proposalService, AuditService auditService,
                                    AskService askService, SpawnService spawnService,
                                    DnaDomainService domainService, DnaGoalService goalService,
                                    GroupMembershipRepository groupMembershipRepository,
                                    AgentRepository agentRepository,
                                    InitiativeRepository initiativeRepository,
                                    DnaGoalRepository goalRepository,
                                    DnaProposalRepository proposalRepository) {
        this.memberService = memberService;
        this.agentService = agentService;
        this.initiativeService = initiativeService;
        this.boardTaskService = boardTaskService;
        this.proposalService = proposalService;
        this.auditService = auditService;
        this.askService = askService;
        this.spawnService = spawnService;
        this.domainService = domainService;
        this.goalService = goalService;
        this.groupMembershipRepository = groupMembershipRepository;
        this.agentRepository = agentRepository;
        this.initiativeRepository = initiativeRepository;
        this.goalRepository = goalRepository;
        this.proposalRepository = proposalRepository;
    }

    /**
     * Full offboarding walk per OFB-001.
     */
    @Transactional
    public Map<String, Object> walkOffboard(String humanId, String successorId, String actor) {
        // OFB-020: Last-admin guard
        long adminCount = memberService.countActiveAdmins();
        Optional<Human> humanOpt = memberService.findHuman(humanId);
        if (humanOpt.isPresent()) {
            Human human = humanOpt.get();
            if ("admin".equals(human.getRbac()) && adminCount <= 1) {
                throw new IllegalStateException("Cannot offboard the last active admin");
            }
        }

        String targetOwner = successorId != null ? successorId : findAnyAdminId();
        int domainsTransferred = 0;
        int agentsReowned = 0;
        int goalsReowned = 0;
        int membershipsCleared = 0;
        int proposalsTransferred = 0;

        // OFB-002: Transfer owned DNA domains
        for (DnaDomain domain : domainService.findAll()) {
            if (humanId.equals(domain.getOwnerHumanId())) {
                String newOwner = targetOwner != null ? targetOwner : humanId;
                domainService.updateOwner(domain.getId(), newOwner, actor);
                domainsTransferred++;
            }
        }

        // OFB-011: Re-own or retire dependent agents
        for (Agent agent : agentService.findByOwner(humanId)) {
            if (targetOwner != null) {
                agent.setOwnerHumanId(targetOwner);
                // Retire personal assistants (CLC-051: mirrored scopes die with member)
                if (agent.getTemplateId() != null && agent.getTemplateId().contains("personal-assistant")) {
                    agentService.retire(agent.getId(), actor);
                } else {
                    agentRepository.save(agent);
                }
                agentsReowned++;
            }
        }

        // OFB-012: Reassign initiatives (sponsor/lead)
        for (Initiative init : initiativeService.findAll()) {
            boolean changed = false;
            if (humanId.equals(init.getSponsor()) && targetOwner != null) {
                init.setSponsor(targetOwner);
                changed = true;
            }
            if (humanId.equals(init.getLead()) && targetOwner != null) {
                init.setLead(targetOwner);
                changed = true;
            }
            if (changed) {
                initiativeRepository.save(init);
            }
        }

        // OFB-013: Re-own or retire owned goals (active only)
        for (DnaGoal goal : goalService.findAllActiveWindowed(Instant.now())) {
            if (humanId.equals(goal.getOwner()) && "active".equals(goal.getStatus()) && targetOwner != null) {
                goal.setOwner(targetOwner);
                goalRepository.save(goal);
                goalsReowned++;
            }
        }

        // OFB-014: Clear deputy references and group memberships
        for (Human h : memberService.findAllActiveHumans()) {
            if (humanId.equals(h.getDeputyMemberId())) {
                h.setDeputyMemberId(null);
                memberService.saveHuman(h);
                membershipsCleared++;
            }
        }
        for (GroupMembership m : groupMembershipRepository.findById_MemberId(humanId)) {
            m.setRemovedAt(Instant.now());
            groupMembershipRepository.save(m);
            membershipsCleared++;
        }

        // OFB-016: Transfer or withdraw authored proposals
        for (DnaProposal prop : proposalService.findAllOpen()) {
            if (humanId.equals(prop.getProposedBy()) && targetOwner != null) {
                prop.setProposedBy(targetOwner);
                proposalRepository.save(prop);
                proposalsTransferred++;
            }
        }

        // OFB-001: Deactivate the human
        humanOpt.ifPresent(human -> {
            human.setDeactivatedAt(Instant.now());
            memberService.saveHuman(human);
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("humanId", humanId);
        result.put("successorId", targetOwner);
        result.put("domainsTransferred", domainsTransferred);
        result.put("agentsReowned", agentsReowned);
        result.put("goalsReowned", goalsReowned);
        result.put("membershipsCleared", membershipsCleared);
        result.put("proposalsTransferred", proposalsTransferred);

        auditService.log(actor, "OFFBOARD_WALK", "human", humanId,
            String.format("{\"successorId\":\"%s\",\"result\":%s}",
                targetOwner != null ? targetOwner : "admin-custody", result));

        return result;
    }

    private String findAnyAdminId() {
        return memberService.findAdmins().stream()
            .map(Human::getId)
            .findFirst()
            .orElse(null);
    }
}
