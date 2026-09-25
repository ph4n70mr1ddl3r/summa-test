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
import java.util.List;
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
    public ResponseEntity<List<Initiative>> listInitiatives(
            @RequestParam(required = false) String status) {
        if (status != null) {
            return ResponseEntity.ok(initiativeService.findByStatus(status));
        }
        return ResponseEntity.ok(initiativeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getInitiative(@PathVariable String id) {
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
            String sponsorClean = sponsorRaw != null ? sponsorRaw.replaceFirst("^[ha]?:", "") : sponsorRaw;
            String leadRaw = body.get("lead");
            String leadClean = leadRaw != null ? leadRaw.replaceFirst("^[ha]?:", "") : leadRaw;
            if (memberService.findHuman(sponsorClean).isEmpty() && memberService.findAgent(sponsorClean).isEmpty()) {
                throw new IllegalArgumentException("sponsor does not reference an existing human or agent: " + sponsorRaw);
            }
            Optional<com.summa.model.Human> sponsorHuman = memberService.findHuman(sponsorClean);
            if (sponsorHuman.isPresent()) {
                if (RbacRole.VIEWER.getValue().equals(sponsorHuman.get().getRbac())) {
                    throw new IllegalArgumentException("Viewers cannot sponsor initiatives");
                }
                if (sponsorHuman.get().getDeactivatedAt() != null) {
                    throw new IllegalArgumentException("Deactivated humans cannot sponsor initiatives");
                }
            } else {
                Optional<com.summa.model.Agent> sponsorAgent = memberService.findAgent(sponsorClean);
                if (sponsorAgent.isPresent() && sponsorAgent.get().isEphemeral()) {
                    throw new IllegalArgumentException("Ephemeral agents cannot sponsor initiatives");
                }
            }
            if (memberService.findHuman(leadClean).isEmpty() && memberService.findAgent(leadClean).isEmpty()) {
                throw new IllegalArgumentException("lead does not reference an existing human or agent: " + leadRaw);
            }
            Optional<com.summa.model.Human> leadHuman = memberService.findHuman(leadClean);
            if (leadHuman.isPresent()) {
                if (RbacRole.VIEWER.getValue().equals(leadHuman.get().getRbac())) {
                    throw new IllegalArgumentException("Viewers cannot lead initiatives");
                }
                if (leadHuman.get().getDeactivatedAt() != null) {
                    throw new IllegalArgumentException("Deactivated humans cannot lead initiatives");
                }
            } else {
                Optional<com.summa.model.Agent> leadAgent = memberService.findAgent(leadClean);
                if (leadAgent.isPresent() && leadAgent.get().isEphemeral()) {
                    throw new IllegalArgumentException("Ephemeral agents cannot lead initiatives");
                }
            }
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
                body.get("dependsOn")
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
        try {
            Initiative initiative = initiativeService.activate(id, actor);
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
        try {
            Initiative initiative = initiativeService.pause(id, actor);
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
        try {
            Initiative initiative = initiativeService.resume(id, actor);
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
        try {
            Initiative initiative = initiativeService.close(id, actor);
            return ResponseEntity.ok(initiative);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }
}
