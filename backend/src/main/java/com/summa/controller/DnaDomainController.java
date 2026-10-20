package com.summa.controller;

import com.summa.enums.DomainAccess;
import com.summa.enums.RbacRole;
import com.summa.service.DnaDomainService;
import com.summa.model.DnaDomain;
import com.summa.model.Human;
import com.summa.service.AuditService;
import com.summa.service.MemberService;
import com.summa.security.WriteGate;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.util.JsonHelpers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/dna/domains")
public class DnaDomainController {
    private final DnaDomainService domainService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final MemberService memberService;

    public DnaDomainController(DnaDomainService domainService, AuditService auditService, WriteGate writeGate,
                               MemberService memberService) {
        this.domainService = domainService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.memberService = memberService;
    }

    @GetMapping
    public ResponseEntity<?> listDomains() {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        return ResponseEntity.ok(domainService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDomain(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<?> auth = ControllerResponses.requireAuth(auditService, memberService, actor);
        if (auth != null) return auth;
        Optional<DnaDomain> entOpt = domainService.findById(id);
        if (entOpt.isPresent()) {
            return ResponseEntity.ok(entOpt.get());
        }
        return ControllerResponses.notFound(auditService, "Domain not found: " + id);
    }

    @PostMapping
    public ResponseEntity<?> createDomain(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Admin access required to create domains");
        }
        try {
            if (body.get("name") == null || body.get("name").isBlank()) {
                throw new IllegalArgumentException("name is required");
            }
            if (body.get("ownerHumanId") == null || body.get("ownerHumanId").isBlank()) {
                throw new IllegalArgumentException("ownerHumanId is required");
            }
            if (body.get("access") != null && !body.get("access").isBlank()) {
                DomainAccess.requireFromValue(body.get("access"));
            }
            String ownerHumanIdRaw = body.get("ownerHumanId");
            String ownerHumanIdClean = JsonHelpers.stripIdPrefix(ownerHumanIdRaw);
            requireEligibleDomainOwner(ownerHumanIdClean, ownerHumanIdRaw);
            DnaDomain domain = domainService.create(
                UUID.randomUUID().toString(),
                body.get("name"),
                ownerHumanIdClean,
                body.get("access"),
                body.get("store"),
                body.containsKey("reviewSlaDays") ? JsonHelpers.parseIntSafe(body.get("reviewSlaDays")) : null,
                body.get("residency"),
                actor
            );
            return ResponseEntity.ok(domain);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archiveDomain(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Admin access required to archive domains");
        }
        try {
            DnaDomain domain = domainService.archive(id, actor);
            return ResponseEntity.ok(domain);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/rename")
    public ResponseEntity<?> renameDomain(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        // DGV-005/API-023: rename is a topology reorg on a structural attribute — admin write,
        // like its sibling archive.
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Admin access required to rename domains");
        }
        try {
            String newName = body.get("name");
            if (newName == null || newName.isBlank()) {
                throw new IllegalArgumentException("name must not be blank");
            }
            DnaDomain domain = domainService.rename(id, newName, actor);
            return ResponseEntity.ok(domain);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PatchMapping("/{id}/owner")
    public ResponseEntity<?> updateOwner(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Admin access required to re-point domain owners");
        }
        try {
            String rawOwner = body.get("ownerHumanId");
            if (rawOwner == null || rawOwner.isBlank()) {
                throw new IllegalArgumentException("ownerHumanId is required");
            }
            String ownerClean = JsonHelpers.stripIdPrefix(rawOwner);
            // DGV-006: the owner must hold role `owner` or `admin` at write, and
            // ORG-020 bars a viewer from the domain-owner post. A domain whose owner
            // cannot pass the owner's own attribute gate would be un-editable by its
            // holder — the owner re-point is a topology act, so it validates here too.
            requireEligibleDomainOwner(ownerClean, rawOwner);
            DnaDomain domain = domainService.updateOwner(id, ownerClean, actor);
            return ResponseEntity.ok(domain);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    /**
     * DGV-006: a domain owner must hold role `owner` or `admin` at write; ORG-020
     * bars a viewer from the domain-owner post and a non-active member from every
     * answerable post. A blank or unknown reference is a validation failure (422),
     * not an integrity violation (409).
     */
    private void requireEligibleDomainOwner(String ownerClean, String rawOwner) {
        if (ownerClean == null || ownerClean.isBlank()) {
            throw new IllegalArgumentException("ownerHumanId is required");
        }
        Optional<Human> ownerOpt = memberService.findHuman(ownerClean);
        if (ownerOpt.isEmpty()) {
            throw new IllegalArgumentException("ownerHumanId does not reference an existing human: " + rawOwner);
        }
        Human owner = ownerOpt.get();
        if (memberService.isViewer(owner)) {
            throw new IllegalStateException("Viewers cannot own a domain: " + rawOwner);
        }
        if (!owner.isActive()) {
            throw new IllegalStateException("Domain owners must be active: " + rawOwner);
        }
        RbacRole role = RbacRole.fromValue(owner.getRbac());
        if (role == null || !role.canManageDomains()) {
            throw new IllegalStateException(
                "Domain owners must hold role owner or admin (DGV-006): " + rawOwner);
        }
    }

    @PatchMapping("/{id}/access")
    public ResponseEntity<?> updateAccess(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        // DGV-005/API-020: access is an owner-editable attribute — domain owner or admin.
        ResponseEntity<Map<String, Object>> ownerGate = requireDomainOwnerOrAdmin(actor, id);
        if (ownerGate != null) return ownerGate;
        try {
            String rawAccess = body.get("access");
            // `dna_domains.access` is NOT NULL with a CHECK on the three policies —
            // an absent value would write NULL and abort the row as an integrity
            // violation, so the door requires the value it is about to store.
            if (rawAccess == null || rawAccess.isBlank()) {
                throw new IllegalArgumentException("access is required");
            }
            DomainAccess.requireFromValue(rawAccess);
            DnaDomain domain = domainService.updateAccess(id, rawAccess, actor);
            return ResponseEntity.ok(domain);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    /**
     * DWP-025: edit a domain's review SLA. Tightening re-derives standing clocks
     * (flagging already-breached proposals); loosening leaves standing clocks
     * untouched. Per DGV-005, review_sla_days is an owner-editable attribute.
     */
    @PatchMapping("/{id}/review-sla")
    public ResponseEntity<?> updateReviewSla(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        // DGV-005/API-020: review_sla_days is an owner-editable attribute — domain owner or admin.
        ResponseEntity<Map<String, Object>> ownerGate = requireDomainOwnerOrAdmin(actor, id);
        if (ownerGate != null) return ownerGate;
        try {
            Integer sla = JsonHelpers.parseIntSafe(body.get("reviewSlaDays"));
            DnaDomain domain = domainService.updateReviewSlaDays(id, sla, actor);
            return ResponseEntity.ok(domain);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    // API-023: topology ops — split and merge
    @SuppressWarnings("unchecked")
    @PostMapping("/{id}/split")
    public ResponseEntity<?> splitDomain(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        // DGV-005/DGV-010: reorgs re-point owners and structural attributes — admin writes.
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Admin access required to split domains");
        }
        try {
            List<?> rawItemIds = body.get("itemIds") instanceof List ? (List<?>) body.get("itemIds") : List.of();
            List<String> itemIds = rawItemIds.stream().filter(o -> o instanceof String).map(o -> (String) o).toList();
            List<?> rawWorkspaceIds = body.get("workspaceIds") instanceof List ? (List<?>) body.get("workspaceIds") : List.of();
            List<String> workspaceIds = rawWorkspaceIds.stream().filter(o -> o instanceof String).map(o -> (String) o).toList();
            List<?> rawProposalIds = body.get("proposalIds") instanceof List ? (List<?>) body.get("proposalIds") : List.of();
            List<String> proposalIds = rawProposalIds.stream().filter(o -> o instanceof String).map(o -> (String) o).toList();

            String ownerHumanId = body.get("ownerHumanId") instanceof String ? (String) body.get("ownerHumanId") : null;
            // DGV-011/DGV-006: a split names each result's owner; the named owner
            // must be able to hold the post, exactly as at creation and at an
            // owner re-point. An undeclared owner inherits the parent's.
            // `dna_domains.owner_human_id` is foreign-key-pinned to humans(id), so
            // the stored reference is the bare id — a keyed union would abort the
            // insert on the foreign key.
            String ownerHumanIdClean = null;
            if (ownerHumanId != null && !ownerHumanId.isBlank()) {
                ownerHumanIdClean = JsonHelpers.stripIdPrefix(ownerHumanId);
                requireEligibleDomainOwner(ownerHumanIdClean, ownerHumanId);
            }
            String access = body.get("access") instanceof String ? (String) body.get("access") : null;
            String store = body.get("store") instanceof String ? (String) body.get("store") : null;
            String sod = body.get("sod") instanceof String ? (String) body.get("sod") : null;
            String residency = body.get("residency") instanceof String ? (String) body.get("residency") : null;
            String namedReaders = body.get("namedReaders") instanceof String ? (String) body.get("namedReaders") : null;

            List<DnaDomain> children = domainService.split(
                id, actor,
                ownerHumanIdClean,
                access,
                store,
                sod,
                residency,
                namedReaders,
                itemIds, workspaceIds, proposalIds
            );
            return ResponseEntity.ok(children);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/merge")
    public ResponseEntity<?> mergeDomain(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        // DGV-005/DGV-014: merges settle topology and reader sets — admin writes.
        if (!memberService.isAdmin(actor)) {
            return ControllerResponses.gate(auditService, actor, "Admin access required to merge domains");
        }
        try {
            String sourceId = body.get("sourceId") instanceof String ? (String) body.get("sourceId") : null;
            if (sourceId == null || sourceId.isBlank()) {
                throw new IllegalArgumentException("sourceId is required");
            }
            String access = body.get("access") instanceof String ? (String) body.get("access") : null;
            String namedReaders = body.get("namedReaders") instanceof String ? (String) body.get("namedReaders") : null;
            DnaDomain survivor = domainService.merge(
                sourceId, id, actor,
                access,
                namedReaders
            );
            return ResponseEntity.ok(survivor);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    /**
     * DGV-005/API-020 row-write authority for owner-editable domain attributes
     * (`access`, `named_readers`, `review_sla_days`): the domain's owner — who must
     * still hold role `owner` or `admin` at write (DGV-006) — or an active admin.
     * Returns null when the write may proceed; a 403 gate otherwise. A missing
     * domain also returns null so the service layer answers with 404 instead.
     */
    private ResponseEntity<Map<String, Object>> requireDomainOwnerOrAdmin(String actor, String domainId) {
        if (memberService.isAdmin(actor)) return null;
        String cleanActor = JsonHelpers.stripIdPrefix(actor);
        if (cleanActor == null) {
            return ControllerResponses.gate(auditService, actor, "Only the domain owner or an admin may edit this domain attribute");
        }
        Optional<DnaDomain> domOpt = domainService.findById(domainId);
        if (domOpt.isEmpty()) return null;
        DnaDomain domain = domOpt.get();
        boolean isOwner = domain.getOwnerHumanId() != null
            && domain.getOwnerHumanId().equals(cleanActor);
        if (isOwner) {
            Optional<Human> humanOpt = memberService.findHuman(cleanActor);
            if (humanOpt.isPresent()) {
                RbacRole role = RbacRole.fromValue(humanOpt.get().getRbac());
                if (role != null && role.canManageDomains()) return null;
            }
        }
        return ControllerResponses.gate(auditService, actor, "Only the domain owner or an admin may edit this domain attribute");
    }

}
