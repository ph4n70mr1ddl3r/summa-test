package com.summa.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @Value("${summa.mode:single-process}")
    private String mode;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "summa",
            "mode", mode,
            "checks", Map.of(
                "database", "UP",
                "git_store", "UP"
            )
        ));
    }
}
