package com.summa.controller;

import com.summa.service.MemoryService;
import com.summa.model.MemoryItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/memory")
public class MemoryController {
    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping
    public ResponseEntity<List<MemoryItem>> listMemory(
            @RequestParam(required = false) String memberId,
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) Boolean tainted) {
        if (memberId != null) {
            return ResponseEntity.ok(memoryService.findByMember(memberId));
        }
        if (workspaceId != null) {
            return ResponseEntity.ok(memoryService.findByWorkspace(workspaceId));
        }
        if (Boolean.TRUE.equals(tainted)) {
            return ResponseEntity.ok(memoryService.findTainted());
        }
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMemory(@PathVariable String id) {
        return memoryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createMemory(@RequestBody Map<String, String> body,
                                           @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            MemoryItem item = memoryService.create(
                body.get("tier"),
                actor,
                body.get("workspaceId"),
                body.get("contentMd"),
                body.get("provenance"),
                Boolean.parseBoolean(body.getOrDefault("tainted", "false"))
            );
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<?> review(@PathVariable String id,
                                      @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            MemoryItem item = memoryService.review(id, actor);
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
