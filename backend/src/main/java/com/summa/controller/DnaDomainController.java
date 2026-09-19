package com.summa.controller;

import com.summa.service.DnaDomainService;
import com.summa.model.DnaDomain;
import com.summa.service.AuditService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.util.JsonHelpers;
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

    public DnaDomainController(DnaDomainService domainService, AuditService auditService, WriteGate writeGate) {
        this.domainService = domainService;
        this.auditService = auditService;
        this.writeGate = writeGate;
    }

    @GetMapping
    public ResponseEntity<List<DnaDomain>> listDomains() {
        return ResponseEntity.ok(domainService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDomain(@PathVariable String id) {
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
        try {
            if (body.get("name") == null || body.get("name").isBlank()) {
                throw new IllegalArgumentException("name is required");
            }
            if (body.get("ownerHumanId") == null || body.get("ownerHumanId").isBlank()) {
                throw new IllegalArgumentException("ownerHumanId is required");
            }
            DnaDomain domain = domainService.create(
                UUID.randomUUID().toString(),
                body.get("name"),
                body.get("ownerHumanId"),
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
            DnaDomain domain = domainService.rename(id, body.get("name") != null && !body.get("name").isBlank() ? body.get("name") : null, actor);
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
        try {
            DnaDomain domain = domainService.updateOwner(id, body.get("ownerHumanId") != null && !body.get("ownerHumanId").isBlank() ? body.get("ownerHumanId") : null, actor);
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
            DnaDomain domain = domainService.updateAccess(id, body.get("access"), actor);
            return ResponseEntity.ok(domain);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    // API-023: topology ops — split and merge
    @PostMapping("/{id}/split")
    public ResponseEntity<?> splitDomain(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            List<String> itemIds = body.get("itemIds") instanceof List ? (List<String>) body.get("itemIds") : List.of();
            List<String> workspaceIds = body.get("workspaceIds") instanceof List ? (List<String>) body.get("workspaceIds") : List.of();
            List<String> proposalIds = body.get("proposalIds") instanceof List ? (List<String>) body.get("proposalIds") : List.of();

            if (!itemIds.isEmpty() && itemIds.stream().anyMatch(itemId -> !(itemId instanceof String))) {
                throw new IllegalArgumentException("itemIds must contain only strings");
            }
            if (!workspaceIds.isEmpty() && workspaceIds.stream().anyMatch(workspaceId -> !(workspaceId instanceof String))) {
                throw new IllegalArgumentException("workspaceIds must contain only strings");
            }
            if (!proposalIds.isEmpty() && proposalIds.stream().anyMatch(proposalId -> !(proposalId instanceof String))) {
                throw new IllegalArgumentException("proposalIds must contain only strings");
            }

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
