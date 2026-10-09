package com.summa.controller;

import com.summa.service.GroupService;
import com.summa.model.Group;
import com.summa.service.AuditService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.constants.Defaults;
import com.summa.service.MemberService;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/org/groups")
public class GroupController {
    private final GroupService groupService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final MemberService memberService;

    public GroupController(GroupService groupService, AuditService auditService, WriteGate writeGate, MemberService memberService) {
        this.groupService = groupService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<?> listGroups() {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        return ResponseEntity.ok(groupService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGroup(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        Optional<Group> entOpt = groupService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Group not found: " + id);
    }

    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        String name = body.get("name");
        String leaderMemberId = body.get("leaderMemberId");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (leaderMemberId == null || leaderMemberId.isBlank()) {
            throw new IllegalArgumentException("leaderMemberId is required");
        }
        try {
            Group group = groupService.create(name, leaderMemberId, actor);
            return ResponseEntity.ok(group);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archiveGroup(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Group group = groupService.archive(id, actor);
            return ResponseEntity.ok(group);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PutMapping("/{id}/leader")
    public ResponseEntity<?> setLeader(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            String leaderId = body.get("leaderMemberId");
            if (leaderId == null || leaderId.isBlank()) {
                throw new IllegalArgumentException("leaderMemberId is required");
            }
            Group group = groupService.setLeader(id, leaderId, actor);
            return ResponseEntity.ok(group);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }
}
