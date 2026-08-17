package com.summa.controller;

import com.summa.service.OrgService;
import com.summa.service.AuditService;
import com.summa.model.Human;
import com.summa.model.AuditEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

@RestController
@RequestMapping("/org")
public class OrgController {
    private final OrgService orgService;
    private final AuditService auditService;

    public OrgController(OrgService orgService, AuditService auditService) {
        this.orgService = orgService;
        this.auditService = auditService;
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<?> bootstrap(@RequestBody Map<String, String> body) {
        try {
            Human human = orgService.bootstrap(
                body.get("name"),
                body.get("email"),
                body.get("rbac")
            );
            return ResponseEntity.ok(Map.of("id", human.getId(), "email", human.getEmail(), "rbac", human.getRbac()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/humans")
    public ResponseEntity<List<Human>> listHumans(@RequestParam(defaultValue = "true") boolean active) {
        List<Human> humans = active ? orgService.findAllActiveHumans() : orgService.findAllHumans();
        return ResponseEntity.ok(humans);
    }

    @GetMapping("/humans/{id}")
    public ResponseEntity<?> getHuman(@PathVariable String id) {
        return orgService.findHuman(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/humans/{id}/rbac")
    public ResponseEntity<?> updateRbac(@PathVariable String id, @RequestBody Map<String, String> body,
                                         @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Human human = orgService.updateRbac(id, body.get("rbac"), actor);
            return ResponseEntity.ok(human);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/humans/{id}/deputy")
    public ResponseEntity<?> setDeputy(@PathVariable String id, @RequestBody Map<String, String> body,
                                        @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Human human = orgService.setDeputy(id, body.get("deputyMemberId"), actor);
            return ResponseEntity.ok(human);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/humans/{id}/offboard")
    public ResponseEntity<?> offboard(@PathVariable String id,
                                       @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Human human = orgService.offboard(id, actor);
            return ResponseEntity.ok(human);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/audit")
    public ResponseEntity<List<AuditEvent>> getAuditLog(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) String objectId) {
        if (objectType != null && objectId != null) {
            return ResponseEntity.ok(orgService.getAuditLogForEntity(objectType, objectId));
        }
        return ResponseEntity.ok(orgService.getAuditLog(limit));
    }
}
