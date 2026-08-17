package com.summa.controller;

import com.summa.service.GroupService;
import com.summa.model.Group;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/org/groups")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public ResponseEntity<List<Group>> listGroups() {
        return ResponseEntity.ok(groupService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGroup(@PathVariable String id) {
        return groupService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody Map<String, String> body,
                                          @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Group group = groupService.create(
                body.get("name"),
                body.get("leaderMemberId")
            );
            return ResponseEntity.ok(group);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archiveGroup(@PathVariable String id,
                                           @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Group group = groupService.archive(id, actor);
            return ResponseEntity.ok(group);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/leader")
    public ResponseEntity<?> setLeader(@PathVariable String id, @RequestBody Map<String, String> body,
                                        @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Group group = groupService.setLeader(id, body.get("leaderMemberId"), actor);
            return ResponseEntity.ok(group);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
