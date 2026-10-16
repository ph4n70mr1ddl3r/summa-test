package com.summa.controller;

import com.summa.enums.DomainAccess;
import com.summa.service.DnaDomainService;
import com.summa.model.DnaDomain;
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
            if (memberService.findHuman(ownerHumanIdClean).isEmpty()) {
                throw new IllegalArgumentException("ownerHumanId does not reference an existing human: " + ownerHumanIdRaw);
            }
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
            String ownerClean = rawOwner != null && !rawOwner.isBlank() ? JsonHelpers.stripIdPrefix(rawOwner) : null;
            DnaDomain domain = domainService.updateOwner(id, ownerClean, actor);
            return ResponseEntity.ok(domain);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PatchMapping("/{id}/access")
    public ResponseEntity<?> updateAccess(@PathVariable String id, @RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            String rawAccess = body.get("access");
            if (rawAccess != null && !rawAccess.isBlank()) {
                DomainAccess.requireFromValue(rawAccess);
            }
            DnaDomain domain = domainService.updateAccess(id, rawAccess, actor);
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
        try {
            List<?> rawItemIds = body.get("itemIds") instanceof List ? (List<?>) body.get("itemIds") : List.of();
            List<String> itemIds = rawItemIds.stream().filter(o -> o instanceof String).map(o -> (String) o).toList();
            List<?> rawWorkspaceIds = body.get("workspaceIds") instanceof List ? (List<?>) body.get("workspaceIds") : List.of();
            List<String> workspaceIds = rawWorkspaceIds.stream().filter(o -> o instanceof String).map(o -> (String) o).toList();
            List<?> rawProposalIds = body.get("proposalIds") instanceof List ? (List<?>) body.get("proposalIds") : List.of();
            List<String> proposalIds = rawProposalIds.stream().filter(o -> o instanceof String).map(o -> (String) o).toList();

            String ownerHumanId = body.get("ownerHumanId") instanceof String ? (String) body.get("ownerHumanId") : null;
            String access = body.get("access") instanceof String ? (String) body.get("access") : null;
            String store = body.get("store") instanceof String ? (String) body.get("store") : null;
            String sod = body.get("sod") instanceof String ? (String) body.get("sod") : null;
            String residency = body.get("residency") instanceof String ? (String) body.get("residency") : null;
            String namedReaders = body.get("namedReaders") instanceof String ? (String) body.get("namedReaders") : null;

            List<DnaDomain> children = domainService.split(
                id, actor,
                ownerHumanId,
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

}
