package com.summa.controller;

import com.summa.service.BackupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/admin/backup")
public class BackupController {
    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @PostMapping
    public ResponseEntity<?> createBackup(@RequestBody Map<String, String> body) {
        try {
            String backupDir = body.getOrDefault("backupDir", System.getProperty("java.io.tmpdir"));
            String path = backupService.createBackup(backupDir);
            return ResponseEntity.ok(Map.of("path", path));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/restore")
    public ResponseEntity<?> restore(@RequestBody Map<String, String> body) {
        try {
            backupService.restore(body.get("backupPath"));
            return ResponseEntity.ok(Map.of("status", "restored"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
