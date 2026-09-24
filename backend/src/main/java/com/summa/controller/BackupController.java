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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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
            Path backupDirPath = validatePathUnder(rawBackupDir, tmpdir, "backupDir");
            String path = backupService.createBackup(backupDirPath.toString());
            auditService.log(actor, "CREATE_BACKUP", "backup", path, null);
            return ResponseEntity.ok(Map.of("path", path));
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IOException e) {
            return ControllerResponses.internalError(auditService, e.getMessage());
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
            Path tmpdir = Paths.get(System.getProperty("java.io.tmpdir")).normalize();
            Path backupFilePath = validatePathUnder(rawPath, tmpdir, "backupPath");
            backupService.restore(backupFilePath.toString());
            auditService.log(actor, "RESTORE_BACKUP", "backup", backupFilePath.toString(), null);
            return ResponseEntity.ok(Map.of("status", "restored"));
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IOException e) {
            return ControllerResponses.internalError(auditService, e.getMessage());
        }
    }

    /**
     * Validate that the given path resolves under the allowed directory.
     * Handles both existing and non-existing paths by walking up to the nearest ancestor.
     */
    private Path validatePathUnder(String rawPath, Path allowedDir, String paramName) throws IOException {
        Path p = Paths.get(rawPath).normalize();
        Path resolved;
        try {
            resolved = p.toRealPath();
        } catch (NoSuchFileException e) {
            Path ancestor = p;
            while (ancestor != null && !Files.exists(ancestor)) {
                ancestor = ancestor.getParent();
            }
            if (ancestor == null) {
                throw new IllegalArgumentException(paramName + " must be under " + allowedDir);
            }
            resolved = allowedDir;
        }
        if (!resolved.startsWith(allowedDir)) {
            throw new IllegalArgumentException(paramName + " must be under " + allowedDir);
        }
        return p;
    }
}
