package com.summa.service;

import com.summa.repository.*;
import com.summa.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class OffboardingWalkService {
    private static final String ADMIN_BROADCAST = "admins";

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
    private final AskRepository askRepository;
    private final BoardTaskRepository boardTaskRepository;
    private final PatRepository patRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OffboardingWalkService(MemberService memberService, AgentService agentService,
                                    InitiativeService initiativeService, BoardTaskService boardTaskService,
                                    DnaProposalService proposalService, AuditService auditService,
                                    AskService askService, SpawnService spawnService,
                                    DnaDomainService domainService, DnaGoalService goalService,
                                    GroupMembershipRepository groupMembershipRepository,
                                    AgentRepository agentRepository,
                                    InitiativeRepository initiativeRepository,
                                    DnaGoalRepository goalRepository,
                                    DnaProposalRepository proposalRepository,
                                    AskRepository askRepository,
                                    BoardTaskRepository boardTaskRepository,
                                    PatRepository patRepository) {
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
        this.askRepository = askRepository;
        this.boardTaskRepository = boardTaskRepository;
        this.patRepository = patRepository;
    }

    /**
     * Full offboarding walk per OFB-001. Last-admin guard is enforced by the caller
     * (OrgService.offboard) in a single transaction; this method assumes the guard
     * passed and proceeds directly to the dependency walk.
     */
    @Transactional
    public Map<String, Object> walkOffboard(String humanId, String successorId, String actor) {
        // Note: last-admin guard is checked in OrgService.offboard(), not here, to avoid TOCTOU.
        Optional<Human> humanOpt = memberService.findHuman(humanId);
        if (humanOpt.isEmpty()) {
            throw new IllegalStateException("Human not found for offboarding: " + humanId);
        }
        String targetOwner = successorId != null ? successorId : findAnyAdminId();
        if (targetOwner == null) {
            throw new IllegalStateException("No successor specified and no active admin found for custody transfer");
        }
        String finalTargetOwner = targetOwner;

        int domainsTransferred = 0;
        int agentsReowned = 0;
        int goalsReowned = 0;
        int membershipsCleared = 0;
        int proposalsTransferred = 0;
        int asksReassigned = 0;
        int tasksReassigned = 0;
        int patsRevoked = 0;

        // OFB-002: Transfer owned DNA domains (include archived — ownership references persist)
        for (DnaDomain domain : domainService.findAllIncludingArchived()) {
            if (humanId.equals(domain.getOwnerHumanId())) {
                domainService.updateOwner(domain.getId(), finalTargetOwner, actor);
                domainsTransferred++;
            }
        }

        // OFB-011: Re-own or retire dependent agents
        for (Agent agent : agentService.findByOwner(humanId)) {
            agent.setOwnerHumanId(finalTargetOwner);
            // Retire personal assistants (CLC-051: mirrored scopes die with member)
            if (agent.getTemplateId() != null && agent.getTemplateId().contains("personal-assistant")) {
                agentService.retire(agent.getId(), actor);
            } else {
                agentRepository.save(agent);
            }
            agentsReowned++;
        }

        // OFB-012: Reassign initiatives (sponsor/lead) — only active ones
        for (Initiative init : initiativeService.findAllActive()) {
            boolean changed = false;
            if (humanId.equals(init.getSponsor())) {
                init.setSponsor(finalTargetOwner);
                changed = true;
            }
            if (humanId.equals(init.getLead())) {
                init.setLead(finalTargetOwner);
                changed = true;
            }
            if (changed) {
                initiativeRepository.save(init);
            }
        }

        // OFB-013: Re-own or retire owned goals (active only)
        for (DnaGoal goal : goalService.findAllActiveWindowed(Instant.now())) {
            if (humanId.equals(goal.getOwner()) && "active".equals(goal.getStatus())) {
                goal.setOwner(finalTargetOwner);
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

        // OFB-016: Transfer authored proposals; leave others untouched
        for (DnaProposal prop : proposalService.findAllOpen()) {
            if (humanId.equals(prop.getProposedBy())) {
                prop.setProposedBy(finalTargetOwner);
                proposalRepository.save(prop);
                proposalsTransferred++;
            }
        }

        // OFB-003: Reassign asks TO the member up the chain; close asks FROM the member with audit note
        for (Ask ask : askRepository.findByStatus("pending")) {
            if (humanId.equals(ask.getTo())) {
                // Reassign up the chain: try deputy first, then admin broadcast
                Optional<Human> targetHuman = memberService.findHuman(humanId);
                if (targetHuman.isPresent() && targetHuman.get().getDeputyMemberId() != null
                        && memberService.findHuman(targetHuman.get().getDeputyMemberId()).isPresent()) {
                    ask.setTo(targetHuman.get().getDeputyMemberId());
                    askRepository.save(ask);
                    asksReassigned++;
                } else {
                    ask.setTo(ADMIN_BROADCAST);
                    askRepository.save(ask);
                    asksReassigned++;
                }
                auditService.logSystem("OFFBOARD_REASSIGN_ASK_TO", "ask", ask.getId(),
                    String.format("{\"newTo\":\"%s\",\"reason\":\"member_departed\"}", ask.getTo()));
            } else if (humanId.equals(ask.getFrom())) {
                // Close asks from the departing member with audit note
                ask.setStatus("withdrawn");
                askRepository.save(ask);
                auditService.logSystem("OFFBOARD_CLOSE_ASK_FROM", "ask", ask.getId(),
                    "{\"reason\":\"member_departed\"}");
                asksReassigned++;
            }
        }

        // OFB-010: Reassign board tasks or return to pool
        for (BoardTask task : boardTaskRepository.findByAssigneeMemberId(humanId)) {
            task.setAssigneeMemberId(finalTargetOwner);
            boardTaskRepository.save(task);
            tasksReassigned++;
            auditService.logSystem("OFFBOARD_REASSIGN_TASK", "board_task", task.getId(),
                String.format("{\"newAssignee\":\"%s\",\"reason\":\"member_departed\"}", finalTargetOwner));
        }

        // OFB-015: Revoke PATs and terminate sessions
        for (Pat pat : patRepository.findByMemberId(humanId)) {
            if (pat.getRevokedAt() == null) {
                pat.setRevokedAt(Instant.now());
                patRepository.save(pat);
                patsRevoked++;
                auditService.logSystem("OFFBOARD_REVOKE_PAT", "pat", pat.getId(),
                    "{\"reason\":\"member_departed\"}");
            }
        }

        // OFB-001: Deactivate the human
        humanOpt.ifPresent(human -> {
            human.setDeactivatedAt(Instant.now());
            memberService.saveHuman(human);
        });
        if (humanOpt.isEmpty()) {
            throw new IllegalStateException("Human not found during offboarding walk: " + humanId);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("humanId", humanId);
        result.put("successorId", finalTargetOwner);
        result.put("domainsTransferred", domainsTransferred);
        result.put("agentsReowned", agentsReowned);
        result.put("goalsReowned", goalsReowned);
        result.put("membershipsCleared", membershipsCleared);
        result.put("proposalsTransferred", proposalsTransferred);
        result.put("asksReassigned", asksReassigned);
        result.put("tasksReassigned", tasksReassigned);
        result.put("patsRevoked", patsRevoked);

        auditService.log(actor, "OFFBOARD_WALK", "human", humanId,
            String.format("{\"successorId\":\"%s\",\"result\":%s}", finalTargetOwner, result));

        return result;
    }

    private String findAnyAdminId() {
        return memberService.findAdmins().stream()
            .map(Human::getId)
            .findFirst()
            .orElse(null);
    }
}
