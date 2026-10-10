package com.summa.controller;

import com.summa.service.InitiativeService;
import com.summa.model.Initiative;
import com.summa.service.AuditService;
import com.summa.service.MemberService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.util.JsonHelpers;
import com.summa.enums.RbacRole;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/initiatives")
public class InitiativeController {
    private final InitiativeService initiativeService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final MemberService memberService;

    public InitiativeController(InitiativeService initiativeService, AuditService auditService, WriteGate writeGate,
                                 MemberService memberService) {
        this.initiativeService = initiativeService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<?> listInitiatives(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        int cappedLimit = Math.min(Math.max(limit, 1), com.summa.constants.Defaults.MAX_LIST_LIMIT);
        if (status != null) {
            return ResponseEntity.ok(initiativeService.findByStatus(status));
        }
        return ResponseEntity.ok(initiativeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getInitiative(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        Optional<Initiative> entOpt = initiativeService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Initiative not found: " + id);
    }

    @PostMapping
    public ResponseEntity<?> createInitiative(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            if (body.get("title") == null || body.get("title").isBlank()) {
                throw new IllegalArgumentException("title is required");
            }
            if (body.get("sponsor") == null || body.get("sponsor").isBlank()) {
                throw new IllegalArgumentException("sponsor is required");
            }
            if (body.get("lead") == null || body.get("lead").isBlank()) {
                throw new IllegalArgumentException("lead is required");
            }
            String sponsorRaw = body.get("sponsor");
            String sponsorClean = JsonHelpers.stripIdPrefix(sponsorRaw);
            String leadRaw = body.get("lead");
            String leadClean = JsonHelpers.stripIdPrefix(leadRaw);
            String generatedId = UUID.randomUUID().toString();
            Instant deadline;
            try {
                deadline = JsonHelpers.parseOptionalInstant(body.get("deadline"), "deadline");
            } catch (IllegalArgumentException e) {
                return ControllerResponses.validation(auditService, e.getMessage());
            }
            Initiative initiative = initiativeService.create(
                generatedId,
                body.get("title"),
                sponsorClean,
                leadClean,
                body.get("goalRef"),
                body.get("decisionRef"),
                deadline,
                body.get("dependsOn"),
                actor
            );
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activate(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!memberService.isAdmin(actor) && !isInitiativeOwnerOrLead(id, actor)) {
            return ControllerResponses.gate(auditService, actor, "Only the sponsor, lead, or an admin can activate this initiative");
        }
        try {
            String actorClean = JsonHelpers.stripIdPrefix(actor);
            Initiative initiative = initiativeService.activate(id, actorClean);
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<?> pause(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!memberService.isAdmin(actor) && !isInitiativeOwnerOrLead(id, actor)) {
            return ControllerResponses.gate(auditService, actor, "Only the sponsor, lead, or an admin can pause this initiative");
        }
        try {
            String actorClean = JsonHelpers.stripIdPrefix(actor);
            Initiative initiative = initiativeService.pause(id, actorClean);
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resume(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!memberService.isAdmin(actor) && !isInitiativeOwnerOrLead(id, actor)) {
            return ControllerResponses.gate(auditService, actor, "Only the sponsor, lead, or an admin can resume this initiative");
        }
        try {
            String actorClean = JsonHelpers.stripIdPrefix(actor);
            Initiative initiative = initiativeService.resume(id, actorClean);
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<?> close(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!memberService.isAdmin(actor) && !isInitiativeOwnerOrLead(id, actor)) {
            return ControllerResponses.gate(auditService, actor, "Only the sponsor, lead, or an admin can close this initiative");
        }
        try {
            String actorClean = JsonHelpers.stripIdPrefix(actor);
            Initiative initiative = initiativeService.close(id, actorClean);
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    private boolean isInitiativeOwnerOrLead(String initiativeId, String actor) {
        Optional<Initiative> opt = initiativeService.findById(initiativeId);
        if (opt.isEmpty()) return false;
        Initiative initiative = opt.get();
        String actorClean = JsonHelpers.stripIdPrefix(actor);
        return actorClean.equals(initiative.getSponsor()) || actorClean.equals(initiative.getLead());
    }
}
