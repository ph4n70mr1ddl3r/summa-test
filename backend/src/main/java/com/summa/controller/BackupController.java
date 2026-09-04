package com.summa.controller;

import com.summa.security.WriteGate;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.enums.RbacRole;
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
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        Optional<Human> actorOpt = orgService.findHuman(actor);
        if (actorOpt.isEmpty() || !RbacRole.ADMIN.getValue().equals(actorOpt.get().getRbac())) {
            var audit = auditService.logSystem("REFUSAL", "backup_create", actor, "Non-admin backup attempt");
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "eligibility", "message", "Backup requires admin role", "audit_event_id", audit.getId()));
        }
        try {
             String rawBackupDir = body.getOrDefault("backupDir", System.getProperty("java.io.tmpdir"));
             Path validRoot = Paths.get(System.getProperty("java.io.tmpdir")).toRealPath();
             Path backupDirPath;
             try {
                 backupDirPath = Paths.get(rawBackupDir).toRealPath();
             } catch (java.io.IOException e) {
                 return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                         .body(Map.of("code", "validation", "message", "backupDir path is inaccessible: " + e.getMessage()));
             }
             if (!backupDirPath.startsWith(validRoot)) {
                 return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                         .body(Map.of("code", "validation", "message", "backupDir must be under tmpdir"));
             }
            String path = backupService.createBackup(backupDirPath.toString());
            auditService.log(actor, "CREATE_BACKUP", "backup", path, null);
            return ResponseEntity.ok(Map.of("path", path));
        } catch (Exception e) {
            var audit = auditService.logSystem("ERROR", "backup_create", actor, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("code", "internal", "message", "Backup failed", "audit_event_id", audit.getId()));
        }
    }

    @PostMapping("/restore")
    public ResponseEntity<?> restore(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        Optional<Human> actorOpt = orgService.findHuman(actor);
        if (actorOpt.isEmpty() || !RbacRole.ADMIN.getValue().equals(actorOpt.get().getRbac())) {
            var audit = auditService.logSystem("REFUSAL", "backup_restore", actor, "Non-admin restore attempt");
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "eligibility", "message", "Restore requires admin role", "audit_event_id", audit.getId()));
        }
        try {
             String rawPath = body.get("backupPath");
             if (rawPath == null || rawPath.isBlank()) {
                 return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                         .body(Map.of("code", "validation", "message", "backupPath is required"));
             }
             Path validRoot = Paths.get(System.getProperty("java.io.tmpdir")).toRealPath();
             Path backupFilePath;
             try {
                 backupFilePath = Paths.get(rawPath).toRealPath();
             } catch (java.io.IOException e) {
                 return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                         .body(Map.of("code", "validation", "message", "backupPath is inaccessible: " + e.getMessage()));
             }
             if (!backupFilePath.startsWith(validRoot)) {
                 return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                         .body(Map.of("code", "validation", "message", "backupPath must be under tmpdir"));
             }
            backupService.restore(backupFilePath.toString());
            auditService.log(actor, "RESTORE_BACKUP", "backup", backupFilePath.toString(), null);
            return ResponseEntity.ok(Map.of("status", "restored"));
        } catch (Exception e) {
            var audit = auditService.logSystem("ERROR", "backup_restore", actor, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("code", "internal", "message", "Restore failed", "audit_event_id", audit.getId()));
        }
    }
}
