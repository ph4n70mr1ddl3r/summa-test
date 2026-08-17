package com.summa.service;

import com.summa.repository.GroupRepository;
import com.summa.model.Group;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final AuditService auditService;

    public GroupService(GroupRepository groupRepository, AuditService auditService) {
        this.groupRepository = groupRepository;
        this.auditService = auditService;
    }

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

    public Group archive(String id, String actor) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + id));
        
        group.setStatus("archived");
        Group saved = groupRepository.save(group);
        auditService.log(actor, "ARCHIVE", "group", id, null);
        return saved;
    }

    public Group setLeader(String id, String leaderMemberId, String actor) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + id));
        
        group.setLeaderMemberId(leaderMemberId);
        Group saved = groupRepository.save(group);
        auditService.log(actor, "SET_LEADER", "group", id, null);
        return saved;
    }
}
