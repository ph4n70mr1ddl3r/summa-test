package com.summa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/info")
public class InfoController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
            "name", "Summa",
            "description", "The operating system for a hybrid human + AI company",
            "version", "0.1.0",
            "buildTime", System.currentTimeMillis(),
            "javaVersion", System.getProperty("java.version"),
            "springBootVersion", "3.4.1"
        ));
    }
}
