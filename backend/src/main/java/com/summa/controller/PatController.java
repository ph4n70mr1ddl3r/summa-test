package com.summa.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.summa.service.PatService;
import com.summa.model.Pat;
import com.summa.service.AuditService;
import com.summa.security.WriteGate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.summa.security.RbacAuthorizationFilter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/pats")
public class PatController {
    private final PatService patService;
    private final AuditService auditService;
    private final WriteGate writeGate;
    private final ObjectMapper objectMapper;

    public PatController(PatService patService, AuditService auditService, WriteGate writeGate,
                         ObjectMapper objectMapper) {
        this.patService = patService;
        this.auditService = auditService;
        this.writeGate = writeGate;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<List<Pat>> listPats(@RequestParam String memberId) {
        return ResponseEntity.ok(patService.findByMember(memberId));
    }

    @PostMapping
    public ResponseEntity<?> createPat(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            int expiryDays = body.containsKey("expiryDays") ? 
                Integer.parseInt(body.get("expiryDays")) : 90;
            
            List<String> scopes = List.of();
            if (body.containsKey("scopes")) {
                String scopesRaw = body.get("scopes").trim();
                if (scopesRaw.isEmpty() || "{}".equals(scopesRaw) || "[]".equals(scopesRaw)) {
                    scopes = List.of();
                } else {
                    try {
                        TypeReference<List<String>> ref = new TypeReference<List<String>>() {};
                        scopes = objectMapper.readValue(scopesRaw, ref);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid scopes format: " + scopesRaw);
                    }
                }
            }
            
            PatService.PatWithToken result = patService.create(
                actor,
                body.get("name"),
                scopes,
                expiryDays
            );
            
            return ResponseEntity.ok(Map.of(
                "id", result.pat().getId(),
                "name", result.pat().getName(),
                "scopes", result.pat().getScopes(),
                "expiresAt", result.pat().getExpiresAt().toString()
            ));
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        }
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<?> revokePat(@PathVariable String id) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            Pat pat = patService.revoke(id, actor);
            return ResponseEntity.ok(pat);
        } catch (IllegalArgumentException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
        }
    }
}
