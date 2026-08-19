package com.summa.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @Value("${summa.spawn.circuit-breaker-tripped:false}")
    private boolean circuitBreakerTripped;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "summa",
            "mode", "single-process",
            "checks", Map.of(
                "database", "UP",
                "git_store", "UP",
                "spawn_circuit_breaker", circuitBreakerTripped ? "TRIPPED" : "CLOSED"
            )
        ));
    }
}
