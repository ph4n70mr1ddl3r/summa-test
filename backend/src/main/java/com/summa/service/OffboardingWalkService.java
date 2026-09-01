package com.summa.service;

import com.summa.repository.AskRepository;
import com.summa.repository.BoardTaskRepository;
import com.summa.repository.DnaGoalRepository;
import com.summa.repository.DnaProposalRepository;
import com.summa.repository.GroupMembershipRepository;
import com.summa.repository.InitiativeRepository;
import com.summa.repository.AgentRepository;
import com.summa.repository.PatRepository;
import com.summa.repository.GroupRepository;
import com.summa.model.Agent;
import com.summa.model.Ask;
import com.summa.model.BoardTask;
import com.summa.model.DnaDomain;
import com.summa.model.DnaGoal;
import com.summa.model.DnaProposal;
import com.summa.model.Group;
import com.summa.model.GroupMembership;
import com.summa.model.Human;
import com.summa.model.Initiative;
import com.summa.model.Pat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OffboardingWalkService {
    // Re-exported from AskService for broadcast-target addressability
    public static final String ADMIN_BROADCAST = "admins";

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
    private final GroupRepository groupRepository;

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
                                    PatRepository patRepository,
                                    GroupRepository groupRepository) {
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
        this.groupRepository = groupRepository;
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
        int groupsLeadershipTransferred = 0;

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

        // OFB-017: Transfer group leadership
        for (Group group : groupRepository.findAll()) {
            if (humanId.equals(group.getLeaderMemberId()) && group.isActive()) {
                group.setLeaderMemberId(finalTargetOwner);
                groupRepository.save(group);
                groupsLeadershipTransferred++;
                auditService.log(actor, "OFFBOARD_TRANSFER_GROUP_LEADER", "group", group.getId(),
                    String.format("{\"newLeader\":\"%s\",\"reason\":\"member_departed\"}", finalTargetOwner));
            }
        }

        // OFB-016: Transfer authored proposals — owned-domain proposals transfer to successor,
        // member-scoped proposals are auto-withdrawn with audit note.
        // We need to distinguish: proposals for domains the departing human owned go to successor;
        // all other open proposals by the departed member are withdrawn.
        for (DnaProposal prop : proposalService.findAllOpen()) {
            if (!humanId.equals(prop.getProposedBy())) continue;
            // Check if this proposal belongs to a domain owned by the departing human
            boolean ownsDomain = false;
            if (prop.getDomainId() != null) {
                Optional<DnaDomain> domOpt = domainService.findById(prop.getDomainId());
                ownsDomain = domOpt.filter(d -> humanId.equals(d.getOwnerHumanId())).isPresent();
            }
            if (ownsDomain) {
                prop.setProposedBy(finalTargetOwner);
                proposalRepository.save(prop);
                proposalsTransferred++;
                auditService.logSystem("OFFBOARD_TRANSFER_PROPOSAL", "dna_proposal", prop.getId(),
                    String.format("{\"domainId\":\"%s\",\"newProposer\":\"%s\"}", prop.getDomainId(), finalTargetOwner));
            } else {
                // Member-scoped proposal: auto-withdraw with audit note
                prop.setStatus("withdrawn");
                proposalRepository.save(prop);
                proposalsTransferred++;
                auditService.logSystem("OFFBOARD_WITHDRAW_PROPOSAL", "dna_proposal", prop.getId(),
                    "{\"reason\":\"member_departed\"}");
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
        result.put("groupsLeadershipTransferred", groupsLeadershipTransferred);

        auditService.log(actor, "OFFBOARD_WALK", "human", humanId,
            String.format("{\"successorId\":\"%s\",\"result\":%s}", finalTargetOwner, result));

        return result;
    }

    /**
     * Demotion walk per OFB-030–033. Runs the same dependency resolution as offboarding
     * but scoped to what the new role can no longer carry. A demotion to viewer strips
     * all ownership, sponsorship, and leadership; a demotion from owner→member sheds
     * only domain ownership. Last-admin guard is enforced by the caller (OrgService.demote).
     */
    @Transactional
    public Map<String, Object> walkDemote(String humanId, String newRbac, String actor) {
        Optional<Human> humanOpt = memberService.findHuman(humanId);
        if (humanOpt.isEmpty()) {
            throw new IllegalStateException("Human not found for demotion: " + humanId);
        }
        Human human = humanOpt.get();
        String currentRbac = human.getRbac();

        // No-op if role is not changing
        if (currentRbac.equals(newRbac)) {
            return Map.of("humanId", humanId, "newRbac", newRbac, "changed", false);
        }

        String targetOwner = findAnyAdminId();
        if (targetOwner == null) {
            throw new IllegalStateException("No active admin found for custody transfer during demotion");
        }

        int domainsTransferred = 0;
        int agentsRetired = 0;
        int goalsRetired = 0;
        int initiativesRepointed = 0;
        int proposalsWithdrawn = 0;
        int asksClosed = 0;
        int tasksReturned = 0;
        int patsRevoked = 0;
        int groupLeadershipsTransferred = 0;

        // OFB-030/033: Transfer owned DNA domains to admin custody
        for (DnaDomain domain : domainService.findAllIncludingArchived()) {
            if (humanId.equals(domain.getOwnerHumanId())) {
                domainService.updateOwner(domain.getId(), targetOwner, actor);
                domainsTransferred++;
            }
        }

        // OFB-030/033: Re-own or retire dependent agents; personal assistants always retire
        for (Agent agent : agentService.findByOwner(humanId)) {
            // CLC-051: demotion to viewer retires the assistant (mirrored viewer scopes are read-only)
            if ("viewer".equals(newRbac) && agent.getTemplateId() != null
                    && agent.getTemplateId().contains("personal-assistant")) {
                agentService.retire(agent.getId(), actor);
                agentsRetired++;
            } else if ("viewer".equals(newRbac)) {
                // Viewer cannot own staff — retire all agents
                agentService.retire(agent.getId(), actor);
                agentsRetired++;
            } else {
                agent.setOwnerHumanId(targetOwner);
                agentRepository.save(agent);
            }
        }

        // OFB-032: Withdraw authored open proposals when new role cannot propose
        for (DnaProposal prop : proposalService.findAllOpen()) {
            if (!humanId.equals(prop.getProposedBy())) continue;
            boolean canPropose = !"viewer".equals(newRbac);
            if (!canPropose) {
                prop.setStatus("withdrawn");
                proposalRepository.save(prop);
                proposalsWithdrawn++;
                auditService.logSystem("DEMOTE_WITHDRAW_PROPOSAL", "dna_proposal", prop.getId(),
                    String.format("{\"humanId\":\"%s\",\"newRbac\":\"%s\"}", humanId, newRbac));
            }
        }

        // OFB-031: Reassign or retire sponsored/led initiatives
        for (Initiative init : initiativeService.findAllActive()) {
            boolean changed = false;
            if (humanId.equals(init.getSponsor())) {
                init.setSponsor(targetOwner);
                changed = true;
            }
            if (humanId.equals(init.getLead())) {
                init.setLead(targetOwner);
                changed = true;
            }
            if (changed) {
                initiativeRepository.save(init);
                initiativesRepointed++;
            }
        }

        // OFB-031: Re-own or retire owned goals (active only)
        for (com.summa.model.DnaGoal goal : goalService.findAllActiveWindowed(Instant.now())) {
            if (humanId.equals(goal.getOwner()) && "active".equals(goal.getStatus())) {
                if ("viewer".equals(newRbac)) {
                    goal.setStatus("retired");
                    goalRepository.save(goal);
                    goalsRetired++;
                } else {
                    goal.setOwner(targetOwner);
                    goalRepository.save(goal);
                    goalsRetired++;
                }
            }
        }

        // OFB-031: Clear deputy references in both directions
        for (Human h : memberService.findAllActiveHumans()) {
            if (humanId.equals(h.getDeputyMemberId())) {
                h.setDeputyMemberId(null);
                memberService.saveHuman(h);
            }
            if (humanId.equals(h.getId()) && h.getDeputyMemberId() != null) {
                h.setDeputyMemberId(null);
                memberService.saveHuman(h);
            }
        }

        // OFB-031: Transfer group leadership posts
        for (com.summa.model.Group group : groupRepository.findAll()) {
            if (humanId.equals(group.getLeaderMemberId()) && group.isActive()) {
                group.setLeaderMemberId(targetOwner);
                groupRepository.save(group);
                groupLeadershipsTransferred++;
                auditService.log(actor, "DEMOTE_TRANSFER_GROUP_LEADER", "group", group.getId(),
                    String.format("{\"newLeader\":\"%s\",\"reason\":\"member_demoted\"}", targetOwner));
            }
        }

        // OFB-031: Close asks to the member up the chain; asks from the member close with audit note
        for (Ask ask : askRepository.findByStatus("pending")) {
            if (humanId.equals(ask.getTo())) {
                ask.setTo(targetOwner);
                askRepository.save(ask);
                asksClosed++;
            } else if (humanId.equals(ask.getFrom())) {
                ask.setStatus("withdrawn");
                askRepository.save(ask);
                asksClosed++;
                auditService.logSystem("DEMOTE_CLOSE_ASK_FROM", "ask", ask.getId(),
                    String.format("{\"humanId\":\"%s\",\"newRbac\":\"%s\"}", humanId, newRbac));
            }
        }

        // OFB-031: Return board-task assignments to pool or reassign
        for (com.summa.model.BoardTask task : boardTaskRepository.findByAssigneeMemberId(humanId)) {
            if ("viewer".equals(newRbac)) {
                task.setAssigneeMemberId(null);
                task.setStatus("open");
                boardTaskRepository.save(task);
                tasksReturned++;
            }
        }

        // OFB-015: Revoke PATs — credential-death on any role reduction
        for (com.summa.model.Pat pat : patRepository.findByMemberId(humanId)) {
            if (pat.getRevokedAt() == null) {
                pat.setRevokedAt(Instant.now());
                patRepository.save(pat);
                patsRevoked++;
            }
        }

        // Update the human's RBAC role
        human.setRbac(newRbac);
        memberService.saveHuman(human);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("humanId", humanId);
        result.put("oldRbac", currentRbac);
        result.put("newRbac", newRbac);
        result.put("domainsTransferred", domainsTransferred);
        result.put("agentsRetired", agentsRetired);
        result.put("goalsRetired", goalsRetired);
        result.put("initiativesRepointed", initiativesRepointed);
        result.put("proposalsWithdrawn", proposalsWithdrawn);
        result.put("asksClosed", asksClosed);
        result.put("tasksReturned", tasksReturned);
        result.put("patsRevoked", patsRevoked);
        result.put("groupsLeadershipTransferred", groupLeadershipsTransferred);
        result.put("changed", true);

        auditService.log(actor, "DEMOTE_WALK", "human", humanId,
            String.format("{\"oldRbac\":\"%s\",\"newRbac\":\"%s\",\"result\":%s}", currentRbac, newRbac, result));

        return result;
    }

    private String findAnyAdminId() {
        return memberService.findAdmins().stream()
            .map(Human::getId)
            .findFirst()
            .orElse(null);
    }
}
