package com.summa.service;

import com.summa.repository.GroupRepository;
import com.summa.model.Group;
import com.summa.service.MemberService;
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
    public Group create(String name, String leaderMemberId) {
        // Check uniqueness
        Optional<Group> existing = groupRepository.findByNameAndStatusNot(name, "archived");
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Group name already exists: " + name);
        }

        Group group = new Group();
        group.setId(UUID.randomUUID().toString());
        group.setName(name);
        group.setLeaderMemberId(leaderMemberId);

        Group saved = groupRepository.save(group);
        auditService.log("system", "CREATE", "group", group.getId(), 
            String.format("{\"name\":\"%s\"}", name));
        return saved;
    }

    public Optional<Group> findById(String id) {
        return groupRepository.findById(id);
    }

    public List<Group> findAll() {
        return groupRepository.findAll();
    }

    @Transactional
    public Group archive(String id, String actor) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + id));
        
        group.setStatus("archived");
        Group saved = groupRepository.save(group);
        auditService.log(actor, "ARCHIVE", "group", id, null);
        return saved;
    }

    @Transactional
    public Group setLeader(String id, String leaderMemberId, String actor) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + id));

        // ORG-042: Validate leader eligibility — active human, not viewer, not ephemeral
        Optional<com.summa.model.Human> leaderOpt = memberService.findHuman(leaderMemberId);
        if (leaderOpt.isEmpty()) {
            throw new IllegalStateException("Leader member not found: " + leaderMemberId);
        }
        com.summa.model.Human leader = leaderOpt.get();
        if (!leader.isActive()) {
            throw new IllegalStateException("Leader must be active: " + leaderMemberId);
        }
        if (memberService.isViewer(leader)) {
            throw new IllegalStateException("Viewers cannot be group leaders: " + leaderMemberId);
        }
        // Check not ephemeral agent
        Optional<com.summa.model.Agent> agentOpt = memberService.findAgent(leaderMemberId);
        if (agentOpt.isPresent() && agentOpt.get().isEphemeral()) {
            throw new IllegalStateException("Ephemeral agents cannot be group leaders: " + leaderMemberId);
        }

        group.setLeaderMemberId(leaderMemberId);
        Group saved = groupRepository.save(group);
        auditService.log(actor, "SET_LEADER", "group", id, null);
        return saved;
    }
}
