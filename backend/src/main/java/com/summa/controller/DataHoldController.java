package com.summa.controller;

import com.summa.service.DataHoldService;
import com.summa.model.DataHold;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/governance/holds")
public class DataHoldController {
    private final DataHoldService holdService;

    public DataHoldController(DataHoldService holdService) {
        this.holdService = holdService;
    }

    @GetMapping
    public ResponseEntity<List<DataHold>> listHolds() {
        return ResponseEntity.ok(holdService.findAllActive());
    }

    @PostMapping
    public ResponseEntity<?> createHold(@RequestBody Map<String, String> body,
                                         @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DataHold hold = holdService.create(
                body.get("kind"),
                body.get("subjectId"),
                body.get("reasonMd"),
                actor
            );
            return ResponseEntity.ok(hold);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<?> releaseHold(@PathVariable String id,
                                           @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        try {
            DataHold hold = holdService.release(id, actor);
            return ResponseEntity.ok(hold);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
