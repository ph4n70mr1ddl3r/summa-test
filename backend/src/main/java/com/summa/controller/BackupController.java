package com.summa.controller;

import com.summa.security.WriteGate;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.service.BackupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/admin/backup")
public class BackupController {
    private final BackupService backupService;
    private final WriteGate writeGate;

    public BackupController(BackupService backupService, WriteGate writeGate) {
        this.backupService = backupService;
        this.writeGate = writeGate;
    }

    @PostMapping
    public ResponseEntity<?> createBackup(@RequestBody Map<String, String> body) {
            String actor = RbacAuthorizationFilter.getCurrentActor();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            String backupDir = body.getOrDefault("backupDir", System.getProperty("java.io.tmpdir"));
            String path = backupService.createBackup(backupDir);
            return ResponseEntity.ok(Map.of("path", path));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "backup failed"));
        }
    }

    @PostMapping("/restore")
    public ResponseEntity<?> restore(@RequestBody Map<String, String> body) {
            String actor = RbacAuthorizationFilter.getCurrentActor();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        try {
            backupService.restore(body.get("backupPath"));
            return ResponseEntity.ok(Map.of("status", "restored"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "restore failed"));
        }
    }
}
