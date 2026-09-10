package com.summa.controller;

import com.summa.security.WriteGate;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.enums.RbacRole;
import com.summa.model.Human;
import com.summa.service.AuditService;
import com.summa.service.BackupService;
import com.summa.service.OrgService;
import org.springframework.http.HttpStatus;
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
            return ControllerResponses.gate(auditService, "Backup requires admin role");
        }
        try {
            String rawBackupDir = body.getOrDefault("backupDir", System.getProperty("java.io.tmpdir"));
            Path tmpdir = Paths.get(System.getProperty("java.io.tmpdir")).normalize().toRealPath();
            Path backupDirPath;
            try {
                backupDirPath = Paths.get(rawBackupDir).normalize().toRealPath();
            } catch (java.nio.file.NoSuchFileException e) {
                backupDirPath = Paths.get(rawBackupDir).normalize();
            }
            if (!backupDirPath.startsWith(tmpdir)) {
                return ControllerResponses.validation(auditService, "backupDir must be under tmpdir");
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
            return ControllerResponses.gate(auditService, "Restore requires admin role");
        }
        try {
            String rawPath = body.get("backupPath");
            if (rawPath == null || rawPath.isBlank()) {
                return ControllerResponses.validation(auditService, "backupPath is required");
            }
            Path tmpdir = Paths.get(System.getProperty("java.io.tmpdir")).normalize().toRealPath();
            Path backupFilePath;
            try {
                backupFilePath = Paths.get(rawPath).normalize().toRealPath();
            } catch (java.nio.file.NoSuchFileException e) {
                backupFilePath = Paths.get(rawPath).normalize();
            }
            if (!backupFilePath.startsWith(tmpdir)) {
                return ControllerResponses.validation(auditService, "backupPath must be under tmpdir");
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
