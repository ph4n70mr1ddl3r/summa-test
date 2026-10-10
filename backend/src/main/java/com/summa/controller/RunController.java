package com.summa.controller;

import com.summa.service.RunService;
import com.summa.model.Run;
import com.summa.service.AuditService;
import com.summa.security.WriteGate;
import com.summa.security.RbacAuthorizationFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.constants.Defaults;
import com.summa.service.GovernanceService;
import com.summa.service.MemberService;
import com.summa.service.WorkspaceService;
import com.summa.util.JsonHelpers;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/runs")
public class RunController {

    private final RunService runService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final GovernanceService governanceService;
    private final MemberService memberService;
    private final WorkspaceService workspaceService;

    public RunController(RunService runService, AuditService auditService, WriteGate writeGate,
                           GovernanceService governanceService, MemberService memberService,
                           WorkspaceService workspaceService) {
        this.runService = runService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.governanceService = governanceService;
        this.memberService = memberService;
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public ResponseEntity<?> listRuns(
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        int cappedLimit = Math.min(Math.max(limit, 1), Defaults.MAX_LIST_LIMIT);
        List<Run> all;
        if (agentId != null) {
            all = runService.findByAgent(agentId, cappedLimit);
        } else if (workspaceId != null) {
            all = runService.findByWorkspace(workspaceId, cappedLimit);
        } else if (status != null) {
            all = runService.findByStatus(status, cappedLimit);
        } else {
            return ResponseEntity.ok(runService.findRecent(cappedLimit));
        }
        return ResponseEntity.ok(all);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRun(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        Optional<Run> entOpt = runService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Run not found: " + id);
    }

    @PostMapping
    public ResponseEntity<?> createRun(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        String agentId = body.get("agentId");
        if (agentId == null || agentId.isBlank()) {
            return ControllerResponses.validation(auditService, "agentId is required");
        }
        agentId = JsonHelpers.stripIdPrefix(agentId);
        if (!runService.agentExists(agentId)) {
            return ControllerResponses.validation(auditService, "agentId does not exist: " + agentId);
        }
        String workspaceId = body.get("workspaceId");
        if (workspaceId != null && !workspaceId.isBlank()) {
            workspaceId = JsonHelpers.stripIdPrefix(workspaceId);
            if (workspaceService.findById(workspaceId).isEmpty()) {
                return ControllerResponses.validation(auditService, "workspaceId does not exist: " + workspaceId);
            }
        }
        try {
            Run run = runService.create(
                agentId,
                workspaceId,
                body.get("initiativeId"),
                body.get("triggerId"),
                body.get("prompt"),
                actor
            );
            return ResponseEntity.ok(run);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startRun(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Run run = runService.start(id);
            return ResponseEntity.ok(run);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeRun(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            String result = body.get("result");
            Long costTokens = null;
            Double costUsd = null;
            if (body.containsKey("costTokens") && body.get("costTokens") != null && !body.get("costTokens").isBlank()) {
                try {
                    costTokens = Long.parseLong(body.get("costTokens"));
                    if (costTokens < 0) throw new IllegalArgumentException("costTokens must be non-negative");
                    if (costTokens > Defaults.MAX_COST_TOKENS) throw new IllegalArgumentException("costTokens exceeds maximum");
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid costTokens: " + body.get("costTokens"));
                }
            }
            if (body.containsKey("costUsd") && body.get("costUsd") != null && !body.get("costUsd").isBlank()) {
                try {
                    costUsd = Double.parseDouble(body.get("costUsd"));
                    if (costUsd < 0) throw new IllegalArgumentException("costUsd must be non-negative");
                    if (Double.isInfinite(costUsd) || Double.isNaN(costUsd)) throw new IllegalArgumentException("costUsd must be finite");
                    if (costUsd > governanceService.resolveSpendCeiling()) throw new IllegalArgumentException("costUsd exceeds spend ceiling");
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid costUsd: " + body.get("costUsd"));
                }
            }

            Run run = runService.complete(id, result, costTokens, costUsd);
            return ResponseEntity.ok(run);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<?> failRun(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Run run = runService.fail(id, body.get("errorMessage"));
            return ResponseEntity.ok(run);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelRun(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Run run = runService.cancel(id);
            return ResponseEntity.ok(run);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resumeRun(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Run run = runService.resume(id);
            return ResponseEntity.ok(run);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Admin access required to view run stats");
        }
        return ResponseEntity.ok(Map.of(
            "queued", runService.countByStatus("queued"),
            "running", runService.countByStatus("running"),
            "completed", runService.countByStatus("completed"),
            "failed", runService.countByStatus("failed"),
            "cancelled", runService.countByStatus("cancelled"),
            "suspended", runService.countByStatus("suspended")
        ));
    }
}
