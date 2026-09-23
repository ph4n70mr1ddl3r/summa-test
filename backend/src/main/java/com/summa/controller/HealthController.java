package com.summa.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @Value("${summa.mode:single-process}")
    private String mode;

    @Value("${summa.git.dna-repo-path:${user.home}/.summa/dna}")
    private String dnaRepoPath;

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
        String gitStatus = checkGitStore();
        Map<String, Object> body = Map.of(
            "status", !dbStatus.equals("UP") || !gitStatus.equals("UP") ? "DEGRADED" : "UP",
            "service", "summa",
            "mode", mode,
            "checks", Map.of(
                "database", dbStatus,
                "git_store", gitStatus
            )
        );
        // Return 503 when degraded so container healthchecks and LBs fail fast.
        if (!dbStatus.equals("UP") || !gitStatus.equals("UP")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
        return ResponseEntity.ok(body);
    }

    private String checkGitStore() {
        try {
            Path path = Paths.get(dnaRepoPath == null || dnaRepoPath.isEmpty() ? System.getProperty("user.home") + "/.summa/dna" : dnaRepoPath);
            if (Files.isDirectory(path) && Files.isReadable(path)) {
                return "UP";
            }
            return "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
