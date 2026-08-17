package com.summa.controller;

import com.summa.service.PatService;
import com.summa.model.Pat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/pats")
public class PatController {
    private final PatService patService;

    public PatController(PatService patService) {
        this.patService = patService;
    }

    @GetMapping
    public ResponseEntity<List<Pat>> listPats(@RequestParam String memberId) {
        return ResponseEntity.ok(patService.findByMember(memberId));
    }

    @PostMapping
    public ResponseEntity<?> createPat(@RequestBody Map<String, String> body,
                                        @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            int expiryDays = body.containsKey("expiryDays") ? 
                Integer.parseInt(body.get("expiryDays")) : 90;
            
            List<String> scopes = List.of();
            if (body.containsKey("scopes")) {
                String scopesStr = body.get("scopes").replace("[", "").replace("]", "")
                    .replace("\"", "").trim();
                if (!scopesStr.isEmpty()) {
                    scopes = List.of(scopesStr.split(","));
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
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<?> revokePat(@PathVariable String id,
                                        @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Pat pat = patService.revoke(id, actor);
            return ResponseEntity.ok(pat);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
