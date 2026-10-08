package com.summa.controller;

import com.summa.service.DnaGoalService;
import com.summa.model.DnaGoal;
import com.summa.model.Human;
import com.summa.model.Agent;
import com.summa.service.AuditService;
import com.summa.service.MemberService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.service.DnaDomainService;
import com.summa.util.JsonHelpers;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/dna/goals")
public class DnaGoalController {
    private final DnaGoalService goalService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final MemberService memberService;
    private final DnaDomainService domainService;

    public DnaGoalController(DnaGoalService goalService, AuditService auditService, WriteGate writeGate,
                              MemberService memberService, DnaDomainService domainService) {
        this.goalService = goalService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.memberService = memberService;
        this.domainService = domainService;
    }

    @GetMapping
    public ResponseEntity<?> listGoals(
            @RequestParam(required = false) String domainId,
            @RequestParam(required = false) String inject) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        if (inject != null) {
            return ResponseEntity.ok(goalService.findActiveInject(inject, Instant.now()));
        }
        if (domainId != null) {
            return ResponseEntity.ok(goalService.findByDomain(domainId));
        }
        return ResponseEntity.ok(goalService.findAllActiveWindowed(Instant.now()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGoal(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        Optional<DnaGoal> entOpt = goalService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Goal not found: " + id);
    }

    @PostMapping
    public ResponseEntity<?> createGoal(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            if (body.get("statementMd") == null || body.get("statementMd").isBlank()) {
                throw new IllegalArgumentException("statementMd is required");
            }
            String ownerRaw = body.get("owner");
            if (ownerRaw == null || ownerRaw.isBlank()) {
                throw new IllegalArgumentException("owner is required");
            }
            String ownerClean = JsonHelpers.stripIdPrefix(ownerRaw);
            Optional<Human> ownerHuman = memberService.findHuman(ownerClean);
            Optional<Agent> ownerAgent = memberService.findAgent(ownerClean);
            if (ownerHuman.isEmpty() && ownerAgent.isEmpty()) {
                throw new IllegalArgumentException("owner does not reference an existing human or agent: " + ownerRaw);
            }
            if (ownerHuman.isPresent() && !ownerHuman.get().isActive()) {
                throw new IllegalArgumentException("owner must be an active member: " + ownerRaw);
            }
            if (ownerAgent.isPresent() && !ownerAgent.get().isActive()) {
                throw new IllegalArgumentException("owner must be an active member: " + ownerRaw);
            }
            String rawDomainId = body.get("domainId");
            if (rawDomainId != null && !rawDomainId.isBlank()) {
                String domainIdClean = JsonHelpers.stripIdPrefix(rawDomainId);
                Optional<com.summa.model.DnaDomain> domOpt = domainService.findById(domainIdClean);
                if (domOpt.isEmpty()) {
                    throw new IllegalArgumentException("Domain not found: " + rawDomainId);
                }
                // SEC-021: Only the domain owner or admin may create goals in a domain
                boolean isDomainOwner = domOpt.get().getOwnerHumanId() != null
                        && domOpt.get().getOwnerHumanId().equals(ownerClean);
                if (!isDomainOwner && !memberService.isAdmin(actor)) {
                    throw new IllegalStateException("Only the domain owner or an admin may create goals in this domain");
                }
            }
            String generatedId = UUID.randomUUID().toString();
            Instant effectiveFrom = JsonHelpers.parseOptionalInstant(body.get("effectiveFrom"), "effectiveFrom");
            if (effectiveFrom == null) effectiveFrom = Instant.now();
            Instant effectiveTo = JsonHelpers.parseOptionalInstant(body.get("effectiveTo"), "effectiveTo");
            if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
                throw new IllegalArgumentException("effectiveTo must not be before effectiveFrom");
            }

            DnaGoal goal = goalService.create(
                generatedId,
                rawDomainId,
                body.get("quarter"),
                body.get("statementMd"),
                ownerClean,
                body.get("inject"),
                effectiveFrom,
                effectiveTo,
                actor
            );
            return ResponseEntity.ok(goal);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            String statusValue = body.get("status");
            if (statusValue == null || statusValue.isBlank()) {
                throw new IllegalArgumentException("status is required");
            }
            if (!"active".equals(statusValue) && !"met".equals(statusValue) && !"missed".equals(statusValue) && !"retired".equals(statusValue)) {
                throw new IllegalArgumentException("Invalid goal status: " + statusValue + ". Must be one of: active, met, missed, retired");
            }
            // SEC-021: Only the goal owner or admin may update goal status
            Optional<DnaGoal> goalOpt = goalService.findById(id);
            if (goalOpt.isEmpty()) {
                return ControllerResponses.notFound(auditService, "Goal not found: " + id);
            }
            DnaGoal goal = goalOpt.get();
            boolean isGoalOwner = goal.getOwner() != null && goal.getOwner().equals(JsonHelpers.stripIdPrefix(actor));
            if (!isGoalOwner && !memberService.isAdmin(actor)) {
                return ControllerResponses.gate(auditService, actor, "Only the goal owner or an admin may update goal status");
            }
            DnaGoal updated = goalService.updateStatus(id, statusValue, actor);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PatchMapping("/{id}/window")
    public ResponseEntity<?> updateWindow(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        Instant effectiveFrom = null;
        Instant effectiveTo = null;
        try {
            effectiveFrom = JsonHelpers.parseOptionalInstant(body.get("effectiveFrom"), "effectiveFrom");
            effectiveTo = JsonHelpers.parseOptionalInstant(body.get("effectiveTo"), "effectiveTo");
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        }
        if (effectiveFrom == null && effectiveTo == null) {
            return ControllerResponses.validation(auditService, "At least one of effectiveFrom or effectiveTo must be provided");
        }
        // SEC-021: Only the goal owner or admin may update goal window
        Optional<DnaGoal> goalOpt = goalService.findById(id);
        if (goalOpt.isEmpty()) {
            return ControllerResponses.notFound(auditService, "Goal not found: " + id);
        }
        DnaGoal goal = goalOpt.get();
        boolean isGoalOwner = goal.getOwner() != null && goal.getOwner().equals(JsonHelpers.stripIdPrefix(actor));
        if (!isGoalOwner && !memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Only the goal owner or an admin may update goal window");
        }
        try {
            DnaGoal updated = goalService.updateWindow(id, effectiveFrom, effectiveTo, actor);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }
}
