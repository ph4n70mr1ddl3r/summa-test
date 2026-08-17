package com.summa.controller;

import com.summa.service.AskService;
import com.summa.model.Ask;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/asks")
public class AskController {
    private final AskService askService;

    public AskController(AskService askService) {
        this.askService = askService;
    }

    @GetMapping
    public ResponseEntity<List<Ask>> listAsks(
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String status) {
        if (to != null) {
            return ResponseEntity.ok(askService.findByTo(to));
        }
        if (status != null) {
            return ResponseEntity.ok(askService.findByStatus(status));
        }
        return ResponseEntity.ok(askService.findAllPending());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAsk(@PathVariable String id) {
        return askService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createAsk(@RequestBody Map<String, String> body,
                                        @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            Instant deadline = Instant.now().plusSeconds(Long.parseLong(body.getOrDefault("deadlineSeconds", "86400")));
            Ask ask = askService.create(
                body.get("kind"),
                actor,
                body.get("to"),
                body.get("payload"),
                body.get("slaTier"),
                body.get("expiryBehavior"),
                body.containsKey("quorumRequired") ? Integer.parseInt(body.get("quorumRequired")) : null,
                deadline,
                body.get("initiativeId"),
                body.get("workspaceId")
            );
            return ResponseEntity.ok(ask);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<?> respond(@PathVariable String id, @RequestBody Map<String, String> body,
                                      @RequestHeader(value = "X-Actor") String actor) {
        try {
            Ask ask = askService.respond(id, actor, body.get("response"));
            return ResponseEntity.ok(ask);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable String id,
                                       @RequestHeader(value = "X-Actor") String actor) {
        try {
            Ask ask = askService.withdraw(id, actor);
            return ResponseEntity.ok(ask);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/expire")
    public ResponseEntity<?> expire(@PathVariable String id) {
        try {
            Ask ask = askService.expire(id);
            return ResponseEntity.ok(ask);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
