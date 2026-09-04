package com.summa.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @Value("${summa.mode:single-process}")
    private String mode;

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        String dbStatus = "DOWN";
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            dbStatus = "UP";
        } catch (Exception e) {
            // database unavailable
        }
        Map<String, Object> body = Map.of(
            "status", dbStatus.equals("UP") ? "UP" : "DEGRADED",
            "service", "summa",
            "mode", mode,
            "checks", Map.of(
                "database", dbStatus,
                "git_store", "UP"
            )
        );
        // Return 503 when degraded so container healthchecks and LBs fail fast.
        if (!dbStatus.equals("UP")) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
        return ResponseEntity.ok(body);
    }
}
