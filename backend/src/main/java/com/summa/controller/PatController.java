package com.summa.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.summa.service.PatService;
import com.summa.model.Pat;
import com.summa.service.AuditService;
import com.summa.model.AuditEvent;
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
                "token", result.token(),
                "scopes", result.pat().getScopes(),
                "expiresAt", result.pat().getExpiresAt().toString()
            ));
        } catch (IllegalArgumentException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "validation", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("code", "validation", "message", e.getMessage(), "audit_event_id", audit.getId()));
        } catch (IllegalStateException e) {
            AuditEvent audit = auditService.logSystem("REFUSAL", "error", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "gate", "message", e.getMessage(), "audit_event_id", audit.getId()));
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
            AuditEvent audit = auditService.logSystem("REFUSAL", "not_found", e.getMessage(), null);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("code", "not_found", "message", e.getMessage(), "audit_event_id", audit.getId()));
        }
    }
}
