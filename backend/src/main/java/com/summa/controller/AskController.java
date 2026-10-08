package com.summa.controller;

import com.summa.service.AskService;
import com.summa.model.Ask;
import com.summa.service.AuditService;
import com.summa.service.MemberService;
import com.summa.security.WriteGate;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.security.RateLimiter;
import com.summa.enums.AskKind;
import com.summa.enums.AskTier;
import com.summa.util.JsonHelpers;
import com.summa.constants.Defaults;
import com.summa.service.OffboardingWalkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.EnumSet;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/asks")
public class AskController {
    private final AskService askService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final MemberService memberService;
    private final RateLimiter rateLimiter;

    private static final Set<String> VALID_ASK_KINDS = EnumSet.allOf(AskKind.class).stream()
        .map(AskKind::getValue).collect(Collectors.toSet());

    private static final Set<String> VALID_ASK_TIERS = EnumSet.allOf(AskTier.class).stream()
        .map(AskTier::getValue).collect(Collectors.toSet());

    private static final Set<String> VALID_EXPIRY_BEHAVIORS = Set.of("deny", "escalate", "reassign");

    private static final int MAX_LIST_LIMIT = Defaults.MAX_LIST_LIMIT;

    public AskController(AskService askService, AuditService auditService, WriteGate writeGate,
                           MemberService memberService, RateLimiter rateLimiter) {
        this.askService = askService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.memberService = memberService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    public ResponseEntity<?> listAsks(
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        int cappedLimit = Math.min(Math.max(limit, 1), MAX_LIST_LIMIT);
        if (to != null) {
            return ResponseEntity.ok(askService.findByTo(to, cappedLimit));
        }
        // Default: show asks to the current actor (inbox), not all pending asks
        return ResponseEntity.ok(askService.findByTo(JsonHelpers.stripIdPrefix(actor), cappedLimit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAsk(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        Optional<Ask> entOpt = askService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Ask not found: " + id);
    }

    @PostMapping
    public ResponseEntity<?> createAsk(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        // Rate limit ask creation per actor to prevent spam
        String rateKey = actor + ":create_ask";
        if (!rateLimiter.allow(rateKey)) {
            long remaining = rateLimiter.getRemainingAttempts(rateKey);
            return ControllerResponses.tooManyRequests(auditService.logSystem("REFUSAL", "ask_create", actor, "Rate limited ask creation"), "Too many asks. Try again later.", remaining);
        }
        try {
            String kind = body.get("kind");
            if (kind == null || kind.isBlank()) {
                throw new IllegalArgumentException("kind is required");
            }
            if (!VALID_ASK_KINDS.contains(kind)) {
                throw new IllegalArgumentException("Invalid kind: " + kind + ". Must be one of: " + String.join(", ", VALID_ASK_KINDS));
            }
            String to = body.get("to");
            if (to == null || to.isBlank()) {
                throw new IllegalArgumentException("to is required");
            }
            // Strip keyed-union prefix (h:/a:) for consistency with other controllers
            String toClean = JsonHelpers.stripIdPrefix(to);
            if (memberService.findHuman(toClean).isEmpty() && memberService.findAgent(toClean).isEmpty()
                    && !OffboardingWalkService.ADMIN_BROADCAST.equals(toClean)) {
                throw new IllegalArgumentException("to does not reference an existing human or agent: " + to);
            }
            String slaTier = body.get("slaTier");
            if (slaTier != null && !slaTier.isBlank()) {
                if (AskTier.fromValue(slaTier) == null) {
                    throw new IllegalArgumentException("Invalid slaTier: " + slaTier + ". Must be one of: " + VALID_ASK_TIERS);
                }
            }
            String expiryBehavior = body.get("expiryBehavior");
            if (expiryBehavior != null && !expiryBehavior.isBlank()) {
                if (!VALID_EXPIRY_BEHAVIORS.contains(expiryBehavior)) {
                    throw new IllegalArgumentException("Invalid expiryBehavior: " + expiryBehavior + ". Must be one of: " + VALID_EXPIRY_BEHAVIORS);
                }
            }
            String deadlineStr = body.get("deadlineSeconds");
            long deadlineSeconds;
            if (deadlineStr != null && !deadlineStr.isBlank()) {
                try {
                    deadlineSeconds = Long.parseLong(deadlineStr);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid deadlineSeconds value");
                }
                if (deadlineSeconds <= 0) {
                    throw new IllegalArgumentException("deadlineSeconds must be positive");
                }
                if (deadlineSeconds > Defaults.MAX_DEADLINE_SECONDS) {
                    throw new IllegalArgumentException("deadlineSeconds must not exceed 365 days");
                }
            } else {
                deadlineSeconds = askService.deriveDeadlineFromTier(slaTier);
                if (deadlineSeconds > Defaults.MAX_DEADLINE_SECONDS) {
                    throw new IllegalArgumentException("Derived deadline exceeds maximum of 365 days");
                }
            }
            Integer quorumRequired = null;
            if (body.containsKey("quorumRequired") && body.get("quorumRequired") != null && !body.get("quorumRequired").isBlank()) {
                quorumRequired = JsonHelpers.parseIntSafe(body.get("quorumRequired"));
                if (quorumRequired == null || quorumRequired < 1) {
                    throw new IllegalArgumentException("quorumRequired must be a positive integer");
                }
            }
            Ask ask = askService.create(
                kind,
                JsonHelpers.stripIdPrefix(actor),
                toClean,
                body.get("payload"),
                slaTier,
                expiryBehavior,
                quorumRequired,
                Instant.now().plusSeconds(deadlineSeconds),
                body.get("initiativeId"),
                body.get("workspaceId")
            );
            return ResponseEntity.ok(ask);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<?> respond(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Ask ask = askService.respond(id, actor, body.get("response"));
            return ResponseEntity.ok(ask);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Ask ask = askService.withdraw(id, actor);
            return ResponseEntity.ok(ask);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/expire")
    public ResponseEntity<?> expire(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Ask ask = askService.expire(id);
            return ResponseEntity.ok(ask);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

}
