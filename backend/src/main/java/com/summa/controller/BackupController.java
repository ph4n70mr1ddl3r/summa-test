package com.summa.controller;

import com.summa.security.WriteGate;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.enums.RbacRole;
import com.summa.model.Human;
import com.summa.service.AuditService;
import com.summa.service.BackupService;
import com.summa.service.OrgService;
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
            Path tmpdir = Paths.get(System.getProperty("java.io.tmpdir")).normalize();
            // toRealPath resolves symlinks; throws if target does not yet exist.
            // Create intermediate dirs so toRealPath always succeeds on the anchor.
            java.nio.file.Files.createDirectories(tmpdir);
            Path tmpdirResolved = tmpdir.toRealPath();
            Path backupDirPath = Paths.get(rawBackupDir).normalize();
            // For non-existent paths, resolve what we can and verify prefix on the real root.
            try {
                backupDirPath = backupDirPath.toRealPath();
            } catch (java.nio.file.NoSuchFileException e) {
                // Path does not exist yet — walk up to the nearest existing ancestor
                // and verify it is within tmpdir before allowing creation.
                Path ancestor = backupDirPath;
                while (ancestor != null && !java.nio.file.Files.exists(ancestor)) {
                    ancestor = ancestor.getParent();
                }
                if (ancestor != null) {
                    try {
                        if (!ancestor.toRealPath().startsWith(tmpdirResolved)) {
                            return ControllerResponses.validation(auditService, "backupDir must be under tmpdir");
                        }
                    } catch (java.nio.file.NoSuchFileException ignored) {
                        // root reached without existing ancestor — reject to be safe
                        return ControllerResponses.validation(auditService, "backupDir must be under tmpdir");
                    }
                } else {
                    return ControllerResponses.validation(auditService, "backupDir must be under tmpdir");
                }
            }
            if (!backupDirPath.toAbsolutePath().normalize().startsWith(tmpdirResolved)) {
                return ControllerResponses.validation(auditService, "backupDir must be under tmpdir");
            }
            String path = backupService.createBackup(backupDirPath.toString());
            auditService.log(actor, "CREATE_BACKUP", "backup", path, null);
            return ResponseEntity.ok(Map.of("path", path));
        } catch (Exception e) {
            return ControllerResponses.internalError(auditService, "Backup failed: " + e.getMessage());
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
            Path restoreTmpdir = Paths.get(System.getProperty("java.io.tmpdir")).normalize();
            java.nio.file.Files.createDirectories(restoreTmpdir);
            Path tmpdirResolved = restoreTmpdir.toRealPath();
            Path backupFilePath = Paths.get(rawPath).normalize();
            try {
                backupFilePath = backupFilePath.toRealPath();
            } catch (java.nio.file.NoSuchFileException e) {
                Path ancestor = backupFilePath;
                while (ancestor != null && !java.nio.file.Files.exists(ancestor)) {
                    ancestor = ancestor.getParent();
                }
                if (ancestor == null) {
                    return ControllerResponses.validation(auditService, "backupPath must be under tmpdir");
                }
                try {
                    if (!ancestor.toRealPath().startsWith(tmpdirResolved)) {
                        return ControllerResponses.validation(auditService, "backupPath must be under tmpdir");
                    }
                } catch (java.nio.file.NoSuchFileException ignored) {
                    return ControllerResponses.validation(auditService, "backupPath must be under tmpdir");
                }
            }
            if (!backupFilePath.toAbsolutePath().normalize().startsWith(tmpdirResolved)) {
                return ControllerResponses.validation(auditService, "backupPath must be under tmpdir");
            }
            backupService.restore(backupFilePath.toString());
            auditService.log(actor, "RESTORE_BACKUP", "backup", backupFilePath.toString(), null);
            return ResponseEntity.ok(Map.of("status", "restored"));
        } catch (Exception e) {
            return ControllerResponses.internalError(auditService, "Restore failed: " + e.getMessage());
        }
    }
}
