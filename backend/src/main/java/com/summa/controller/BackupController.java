package com.summa.controller;

import com.summa.exception.EntityNotFoundException;
import com.summa.security.WriteGate;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.service.AuditService;
import com.summa.service.BackupService;
import com.summa.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/admin/backup")
public class BackupController {
    private final BackupService backupService;
    private final WriteGate writeGate;
    private final MemberService memberService;
    private final AuditService auditService;

    public BackupController(BackupService backupService, WriteGate writeGate,
                            MemberService memberService, AuditService auditService) {
        this.backupService = backupService;
        this.writeGate = writeGate;
        this.memberService = memberService;
        this.auditService = auditService;
    }

    @PostMapping
    public ResponseEntity<?> createBackup(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        requireAdmin(actor);
        try {
            String rawBackupDir = body.get("backupDir");
            if (rawBackupDir == null || rawBackupDir.isBlank()) {
                rawBackupDir = System.getProperty("java.io.tmpdir");
            }
            Path tmpdir = Paths.get(System.getProperty("java.io.tmpdir")).normalize();
            Path backupDirPath = validatePathUnder(rawBackupDir, tmpdir, "backupDir");
            String path = backupService.createBackup(backupDirPath.toString());
            auditService.log(actor, "CREATE_BACKUP", "backup", path, null);
            return ResponseEntity.ok(Map.of("path", path));
        } catch (IllegalArgumentException e) {
            return ControllerResponses.validation(auditService, e.getMessage());
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        } catch (IOException e) {
            return ControllerResponses.internalError(auditService, e.getMessage());
        }
    }

    @PostMapping("/restore")
    public ResponseEntity<?> restore(@RequestBody Map<String, String> body) {
        String actor = RbacAuthorizationFilter.getCurrentActorOrDefault();
        ResponseEntity<Map<String, Object>> gate = writeGate.enforce(actor);
        if (gate != null) return gate;
        requireAdmin(actor);
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
        } catch (IllegalStateException e) {
            return ControllerResponses.gate(auditService, e.getMessage());
        } catch (IOException e) {
            return ControllerResponses.internalError(auditService, e.getMessage());
        } catch (EntityNotFoundException e) {
            return ControllerResponses.notFound(auditService, e.getMessage());
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
            resolved = ancestor;
        }
        if (!resolved.startsWith(allowedDir)) {
            throw new IllegalArgumentException(paramName + " must be under " + allowedDir);
        }
        return resolved;
    }

    private void requireAdmin(String actor) {
        if (!memberService.isAdmin(actor)) {
            throw new IllegalStateException("Admin access required");
        }
    }
}
