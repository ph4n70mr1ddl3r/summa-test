package com.summa.controller;

import com.summa.security.WriteGate;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.service.BackupService;
import com.summa.service.OrgService;
import com.summa.service.AuditService;
import com.summa.model.Human;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin/backup")
public class BackupController {
    private final BackupService backupService;
    private final WriteGate writeGate;
    private final OrgService orgService;
    private final AuditService auditService;

    public BackupController(BackupService backupService, WriteGate writeGate,
                            OrgService orgService, AuditService auditService) {
        this.backupService = backupService;
        this.writeGate = writeGate;
        this.orgService = orgService;
        this.auditService = auditService;
    }

    @PostMapping
    public ResponseEntity<?> createBackup(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActor();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        Optional<Human> actorOpt = orgService.findHuman(actor);
        if (actorOpt.isEmpty() || !"admin".equals(actorOpt.get().getRbac())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "admin_only", "message", "Backup requires admin role"));
        }
        try {
            String rawBackupDir = body.getOrDefault("backupDir", System.getProperty("java.io.tmpdir"));
            Path backupDirPath = Paths.get(rawBackupDir).normalize();
            Path validRoot = Paths.get(System.getProperty("java.io.tmpdir")).normalize();
            if (!backupDirPath.startsWith(validRoot)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                        .body(Map.of("code", "validation", "message", "backupDir must be under tmpdir"));
            }
            String path = backupService.createBackup(backupDirPath.toString());
            auditService.log(actor, "CREATE_BACKUP", "backup", path, null);
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
        Optional<Human> actorOpt = orgService.findHuman(actor);
        if (actorOpt.isEmpty() || !"admin".equals(actorOpt.get().getRbac())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "admin_only", "message", "Restore requires admin role"));
        }
        try {
            String rawPath = body.get("backupPath");
            if (rawPath == null || rawPath.isBlank()) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                        .body(Map.of("code", "validation", "message", "backupPath is required"));
            }
            Path backupFilePath = Paths.get(rawPath).normalize();
            Path validRoot = Paths.get(System.getProperty("java.io.tmpdir")).normalize();
            if (!backupFilePath.startsWith(validRoot)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                        .body(Map.of("code", "validation", "message", "backupPath must be under tmpdir"));
            }
            backupService.restore(backupFilePath.toString());
            auditService.log(actor, "RESTORE_BACKUP", "backup", backupFilePath.toString(), null);
            return ResponseEntity.ok(Map.of("status", "restored"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "restore failed"));
        }
    }
}
