package com.summa.controller;

import com.summa.service.MemoryService;
import com.summa.model.MemoryItem;
import com.summa.service.AuditService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.enums.MemoryTier;
import com.summa.constants.Defaults;
import com.summa.service.MemberService;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/memory")
public class MemoryController {
    private final MemoryService memoryService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final MemberService memberService;

    public MemoryController(MemoryService memoryService, AuditService auditService, WriteGate writeGate, MemberService memberService) {
        this.memoryService = memoryService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<?> listMemory(
            @RequestParam(required = false) String memberId,
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) Boolean tainted,
            @RequestParam(defaultValue = "50") int limit) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        int cappedLimit = Math.min(Math.max(limit, 1), Defaults.MAX_LIST_LIMIT);
        if (memberId != null) {
            return ResponseEntity.ok(memoryService.findByMember(memberId, cappedLimit));
        }
        if (workspaceId != null) {
            return ResponseEntity.ok(memoryService.findByWorkspace(workspaceId, cappedLimit));
        }
        if (Boolean.TRUE.equals(tainted)) {
            return ResponseEntity.ok(memoryService.findTainted(cappedLimit));
        }
        return ResponseEntity.ok(memoryService.findAll(cappedLimit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMemory(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        Optional<MemoryItem> entOpt = memoryService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Memory item not found: " + id);
    }

    @PostMapping
    public ResponseEntity<?> createMemory(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            String tier = body.get("tier");
            if (tier == null || tier.isBlank()) {
                throw new IllegalArgumentException("tier is required");
            }
            try {
                MemoryTier parsedTier = MemoryTier.requireFromValue(tier);
                tier = parsedTier.getValue();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
            MemoryItem item = memoryService.create(
                tier,
                actor,
                body.get("workspaceId"),
                body.get("contentMd"),
                body.get("provenance"),
                Boolean.parseBoolean(body.getOrDefault("tainted", "false"))
            );
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<?> review(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            MemoryItem item = memoryService.review(id, actor);
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }
}
