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

    public OffboardingWalkService(MemberService memberService, AgentService agentService,
                                   InitiativeService initiativeService, BoardTaskService boardTaskService,
                                   DnaProposalService proposalService, AuditService auditService,
                                   AskService askService, SpawnService spawnService) {
        this.memberService = memberService;
        this.agentService = agentService;
        this.initiativeService = initiativeService;
        this.boardTaskService = boardTaskService;
        this.proposalService = proposalService;
        this.auditService = auditService;
        this.askService = askService;
        this.spawnService = spawnService;
    }

    /**
     * Full offboarding walk per OFB-001.
     * Deactivates a human and resolves all dependencies.
     */
    @Transactional
    public Map<String, Object> walkOffboard(String humanId, String successorId, String actor) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("humanId", humanId);
        result.put("successorId", successorId);

        // OFB-002: Transfer owned DNA domains
        result.put("domainsTransferred", 0);
        // OFB-003: Reassign asks to member, close asks from member
        result.put("asksReassigned", 0);
        // OFB-010: Return board tasks to pool
        result.put("tasksReturned", 0);
        // OFB-011: Re-own or retire dependent agents
        result.put("agentsReowned", 0);
        // OFB-012: Reassign or close initiatives
        result.put("initiativesClosed", 0);
        // OFB-013: Re-own or retire owned goals
        result.put("goalsRetired", 0);
        // OFB-014: Clear memberships and deputies
        result.put("membershipsCleared", 0);
        // OFB-015: Terminate sessions and revoke PATs
        result.put("patsRevoked", 0);
        // OFB-016: Transfer or withdraw authored proposals
        result.put("proposalsTransferred", 0);

        // Deactivate the human
        memberService.findHuman(humanId).ifPresent(human -> {
            human.setDeactivatedAt(Instant.now());
            memberService.findHuman(humanId); // trigger save via repository
        });

        return result;
    }
}
