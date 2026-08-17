package com.summa.controller;

import com.summa.service.DnaDomainService;
import com.summa.model.DnaDomain;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dna/domains")
public class DnaDomainController {
    private final DnaDomainService domainService;

    public DnaDomainController(DnaDomainService domainService) {
        this.domainService = domainService;
    }

    @GetMapping
    public ResponseEntity<List<DnaDomain>> listDomains() {
        return ResponseEntity.ok(domainService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDomain(@PathVariable String id) {
        return domainService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createDomain(@RequestBody Map<String, String> body,
                                           @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaDomain domain = domainService.create(
                body.get("id"),
                body.get("name"),
                body.get("ownerHumanId"),
                body.get("access"),
                body.get("store"),
                body.containsKey("reviewSlaDays") ? Integer.parseInt(body.get("reviewSlaDays")) : null,
                body.get("residency")
            );
            return ResponseEntity.ok(domain);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archiveDomain(@PathVariable String id,
                                            @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaDomain domain = domainService.archive(id, actor);
            return ResponseEntity.ok(domain);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/owner")
    public ResponseEntity<?> updateOwner(@PathVariable String id, @RequestBody Map<String, String> body,
                                          @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaDomain domain = domainService.updateOwner(id, body.get("ownerHumanId"), actor);
            return ResponseEntity.ok(domain);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/access")
    public ResponseEntity<?> updateAccess(@PathVariable String id, @RequestBody Map<String, String> body,
                                           @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DnaDomain domain = domainService.updateAccess(id, body.get("access"), actor);
            return ResponseEntity.ok(domain);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
