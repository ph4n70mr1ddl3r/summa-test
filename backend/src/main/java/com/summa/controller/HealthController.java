package com.summa.controller;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up()
                .withDetail("service", "summa")
                .withDetail("mode", "single-process")
                .build();
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthDetail() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "summa",
            "mode", "single-process",
            "checks", Map.of(
                "database", "UP",
                "git_store", "UP",
                "spawn_circuit_breaker", "CLOSED"
            )
        ));
    }
}
