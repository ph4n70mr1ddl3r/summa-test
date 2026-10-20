package com.summa.service;

import com.summa.repository.GroupRepository;
import com.summa.model.Group;
import com.summa.model.Human;
import com.summa.model.Agent;
import com.summa.service.MemberService;
import com.summa.exception.EntityNotFoundException;
import com.summa.util.JsonHelpers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final AuditService auditService;
    private final MemberService memberService;

    public GroupService(GroupRepository groupRepository, AuditService auditService,
                        MemberService memberService) {
        this.groupRepository = groupRepository;
        this.auditService = auditService;
        this.memberService = memberService;
    }

    @Transactional
    public Group create(String name, String leaderMemberId, String actor) {
        // Check uniqueness
        Optional<Group> existing = groupRepository.findByNameAndStatusNot(name, "archived");
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("Group name already exists: " + name);
        }

        // ORG-042: the Leader post is guarded at write — the same guard the
        // set-leader door applies. Creating a group with a viewer, a non-active
        // member or an ephemeral worker as Leader would route to an unanswerable
        // addressee, so the guard is not a set-leader-only rule.
        String leaderKeyed = null;
        if (leaderMemberId != null && !leaderMemberId.isBlank()) {
            leaderKeyed = requireEligibleLeader(leaderMemberId);
        }

        Group group = new Group();
        group.setId(UUID.randomUUID().toString());
        group.setName(name);
        // DAT-120: leader_member_id is stored as a keyed union (h:<id> for a human).
        group.setLeaderMemberId(leaderKeyed);

        Group saved = groupRepository.save(group);
        auditService.log(actor, "CREATE", "group", group.getId(),
            String.format("{\"name\":%s}", JsonHelpers.jsonString(name)));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Group> findById(String id) {
        return groupRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Group> findAll() {
        return groupRepository.findAll();
    }

    @Transactional
    public Group archive(String id, String actor) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group not found: " + id));
        
        group.setStatus("archived");
        Group saved = groupRepository.save(group);
        auditService.log(actor, "ARCHIVE", "group", id, null);
        return saved;
    }

    @Transactional
    public Group setLeader(String id, String leaderMemberId, String actor) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group not found: " + id));

        // ORG-042: Validate leader eligibility — active human, not viewer, not ephemeral
        String leaderKeyed = requireEligibleLeader(leaderMemberId);

        // DAT-120: the human leader is re-prefixed to the stored keyed-union form.
        group.setLeaderMemberId(leaderKeyed);
        Group saved = groupRepository.save(group);
        auditService.log(actor, "SET_LEADER", "group", id, null);
        return saved;
    }

    /**
     * ORG-042: the Leader post is guarded at write — a viewer human or any
     * non-active member is refused (routing addresses the Leader; the Leader must
     * be answerable), and an ephemeral worker is refused by the mortality pin.
     *
     * <p>ORG-040: groups mix humans and agents and a local agent may act as
     * Leader for execution routing, so the Leader resolves against either member
     * table; the stored form carries the resolved kind (DAT-120).
     *
     * @return the keyed-union form of the eligible leader (DAT-120)
     */
    private String requireEligibleLeader(String leaderMemberId) {
        String cleanLeader = JsonHelpers.stripIdPrefix(leaderMemberId);
        if (cleanLeader == null || cleanLeader.isBlank()) {
            throw new IllegalArgumentException("leaderMemberId is required");
        }
        Optional<Human> leaderOpt = memberService.findHuman(cleanLeader);
        if (leaderOpt.isPresent()) {
            Human leader = leaderOpt.get();
            if (!leader.isActive()) {
                throw new IllegalStateException("Leader must be active: " + leaderMemberId);
            }
            if (memberService.isViewer(leader)) {
                throw new IllegalStateException("Viewers cannot be group leaders: " + leaderMemberId);
            }
            // DAT-120: the human leader is stored in the keyed-union form.
            return "h:" + leader.getId();
        }
        // ORG-040: a local agent may act as Leader for execution routing.
        Optional<Agent> agentOpt = memberService.findAgent(cleanLeader);
        if (agentOpt.isPresent()) {
            Agent leader = agentOpt.get();
            if (leader.isEphemeral()) {
                throw new IllegalStateException("Ephemeral agents cannot be group leaders: " + leaderMemberId);
            }
            if (!leader.isActive()) {
                throw new IllegalStateException("Leader must be active: " + leaderMemberId);
            }
            // DAT-120: an agent leader keeps its `a:` kind — the stored reference
            // must resolve against the live row of its own keyed kind.
            return "a:" + leader.getId();
        }
        throw new EntityNotFoundException("Leader member not found: " + leaderMemberId);
    }
}
