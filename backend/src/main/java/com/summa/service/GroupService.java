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

        Group group = new Group();
        group.setId(UUID.randomUUID().toString());
        group.setName(name);
        // DAT-120: leader_member_id is stored as a keyed union (h:<id> for a human).
        group.setLeaderMemberId(normalizeLeader(leaderMemberId));

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
        String cleanLeader = JsonHelpers.stripIdPrefix(leaderMemberId);
        Optional<Human> leaderOpt = memberService.findHuman(cleanLeader);
        if (leaderOpt.isEmpty()) {
            throw new EntityNotFoundException("Leader member not found: " + leaderMemberId);
        }
        Human leader = leaderOpt.get();
        if (!leader.isActive()) {
            throw new IllegalStateException("Leader must be active: " + leaderMemberId);
        }
        if (memberService.isViewer(leader)) {
            throw new IllegalStateException("Viewers cannot be group leaders: " + leaderMemberId);
        }
        // Check not ephemeral agent
        Optional<Agent> agentOpt = memberService.findAgent(cleanLeader);
        if (agentOpt.isPresent() && agentOpt.get().isEphemeral()) {
            throw new IllegalStateException("Ephemeral agents cannot be group leaders: " + leaderMemberId);
        }

        // DAT-120: the human leader is re-prefixed to the stored keyed-union form.
        group.setLeaderMemberId(normalizeLeader(leaderMemberId));
        Group saved = groupRepository.save(group);
        auditService.log(actor, "SET_LEADER", "group", id, null);
        return saved;
    }

    /**
     * DAT-120: normalize a caller-supplied leader reference to the stored
     * keyed-union form (h:<humans.id>).
     */
    private static String normalizeLeader(String leaderMemberId) {
        String clean = JsonHelpers.stripIdPrefix(leaderMemberId);
        return clean == null ? leaderMemberId : "h:" + clean;
    }
}
