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
        return ResponseEntity.ok(Map.of(
            "status", dbStatus.equals("UP") ? "UP" : "DEGRADED",
            "service", "summa",
            "mode", mode,
            "checks", Map.of(
                "database", dbStatus,
                "git_store", "UP"
            )
        ));
    }
}
