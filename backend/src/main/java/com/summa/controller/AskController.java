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
import java.util.List;
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
        int cappedLimit = Math.min(Math.max(limit, 1), Defaults.MAX_LIST_LIMIT);
        // Ask "to"/"from" values are stored as keyed unions; normalize the inbox
        // target so the query matches the stored representation.
        String target = to != null ? normalizeMemberRef(to) : normalizeMemberRef(actor);
        if (target == null) {
            // The reference resolves to no live member — there is no such inbox.
            return ResponseEntity.ok(java.util.List.of());
        }
        List<Ask> result;
        if (status != null && !status.isBlank()) {
            result = askService.findByToAndStatus(target, status, cappedLimit);
        } else {
            // Default: show pending asks to the target (the current actor's inbox)
            result = askService.findByTo(target, cappedLimit);
        }
        // DAT-080/ASK-055: the reserved `admins` broadcast renders against the live
        // admin set — "every active admin, evaluated at render and respond time".
        // Without this the broadcast is invisible to the very admins it addresses:
        // the org-stall alert, the proposal SLA escalation and the spawn-gate
        // fallback all file to `admins` and no admin could ever see or answer them.
        // An explicit `to` is the caller naming a box, so the broadcast is added
        // only to the actor's own inbox.
        if (to == null && memberService.isAdmin(actor)) {
            result = mergeBroadcast(result, status, cappedLimit);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * DAT-080/ASK-055: union the admin broadcast into an admin's own inbox. The
     * broadcast is addressed to the live admin set, so it renders for every active
     * admin; the union is capped like any other list response.
     */
    private List<Ask> mergeBroadcast(List<Ask> own, String status, int cappedLimit) {
        List<Ask> broadcast = (status != null && !status.isBlank())
                ? askService.findByToAndStatus(OffboardingWalkService.ADMIN_BROADCAST, status, cappedLimit)
                : askService.findByTo(OffboardingWalkService.ADMIN_BROADCAST, cappedLimit);
        if (broadcast.isEmpty()) return own;
        Map<String, Ask> merged = new java.util.LinkedHashMap<>();
        for (Ask ask : own) merged.put(ask.getId(), ask);
        for (Ask ask : broadcast) merged.putIfAbsent(ask.getId(), ask);
        List<Ask> result = new java.util.ArrayList<>(merged.values());
        result.sort(java.util.Comparator.comparing(
                a -> a.getDeadline() != null ? a.getDeadline() : Instant.EPOCH));
        return result.size() > cappedLimit ? result.subList(0, cappedLimit) : result;
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
            // Strip keyed-union prefix (h:/a:) for consistency with other controllers.
            // The asks.from/asks.to CHECK constraint only accepts 'system', 'admins',
            // or a keyed union, so a plain member id must be re-prefixed.
            String toClean = normalizeMemberRef(to);
            String fromClean = normalizeMemberRef(actor);
            if (toClean == null) {
                throw new IllegalArgumentException("to does not reference an existing human or agent: " + to);
            }
            if (fromClean == null) {
                throw new IllegalArgumentException("actor does not reference an existing member: " + actor);
            }
            // ORG-020: filing an ask is a member post — a viewer is never an ask
            // target and a non-active member is refused every answerable post at
            // write. The reserved `admins` broadcast is not member-addressed and
            // stays exempt.
            memberService.requireEligibleAskTarget(toClean);
            // ASK-031/DAT-120: the reserved system originator is never a target, and
            // `asks.to`'s CHECK admits only `admins` or a keyed union — a `system`
            // target would abort the insert as an integrity violation.
            if (Defaults.SYSTEM_ACTOR.equals(toClean)) {
                throw new IllegalArgumentException("to must not be the system originator");
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
            // API-061: validation before constraint — asks.initiative_id and
            // asks.workspace_id are foreign-key-pinned, so an unknown reference
            // would abort the insert as an integrity violation (a 409) instead of
            // the validation refusal the API owes.
            String initiativeId = body.get("initiativeId");
            String workspaceId = body.get("workspaceId");
            askService.requireLiveReferences(initiativeId, workspaceId);
            Ask ask = askService.create(
                kind,
                fromClean,
                toClean,
                body.get("payload"),
                slaTier,
                expiryBehavior,
                quorumRequired,
                Instant.now().plusSeconds(deadlineSeconds),
                initiativeId,
                workspaceId
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
            String response = body.get("response");
            if (response == null || response.isBlank()) {
                throw new IllegalArgumentException("response is required");
            }
            // ASK-050: the decision is the ask's accept/deny vocabulary. An absent
            // decision is an acceptance (the historical free-text response); a
            // deny closes the ask denied immediately.
            String decision = body.get("decision");
            // The asks table stores from/to as keyed unions (h:<id> / a:<id>),
            // so the human actor is re-prefixed after stripping.
            String responderRef = normalizeMemberRef(actor);
            if (responderRef == null) {
                throw new IllegalArgumentException("actor does not reference an existing member: " + actor);
            }
            Ask ask = askService.respond(id, responderRef, response, decision);
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
            String originatorRef = normalizeMemberRef(actor);
            if (originatorRef == null) {
                throw new IllegalArgumentException("actor does not reference an existing member: " + actor);
            }
            Ask ask = askService.withdraw(id, originatorRef);
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
    /**
     * Normalize a member reference to the stored keyed-union representation
     * used in asks.from / asks.to. The kind is resolved by live lookup, so an
     * agent addressee keeps its {@code a:} kind (a mislabelled kind makes the
     * ask resolve against the wrong live table). "system" and "admins" pass
     * through unchanged.
     *
     * @return the keyed-union reference, or null when the reference resolves to
     *         no live member
     */
    private String normalizeMemberRef(String ref) {
        String clean = JsonHelpers.stripIdPrefix(ref);
        if (clean == null) return null;
        if (OffboardingWalkService.ADMIN_BROADCAST.equals(clean)
                || Defaults.SYSTEM_ACTOR.equals(clean)) {
            return clean;
        }
        return memberService.toKeyedMemberRef(clean);
    }

}
