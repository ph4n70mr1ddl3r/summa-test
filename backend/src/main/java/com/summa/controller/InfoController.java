package com.summa.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/info")
public class InfoController {

    @Value("${project.version:unknown}")
    private String projectVersion;

    @GetMapping
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
            "name", "Summa",
            "description", "The operating system for a hybrid human + AI company",
            "version", projectVersion
        ));
    }
}
