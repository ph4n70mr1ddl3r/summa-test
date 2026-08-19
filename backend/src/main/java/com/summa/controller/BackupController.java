package com.summa.controller;

import com.summa.security.WriteGate;
import com.summa.security.RbacAuthorizationFilter;
import com.summa.service.BackupService;
import com.summa.service.OrgService;
import com.summa.model.Human;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin/backup")
public class BackupController {
    private final BackupService backupService;
    private final WriteGate writeGate;
    private final OrgService orgService;

    public BackupController(BackupService backupService, WriteGate writeGate, OrgService orgService) {
        this.backupService = backupService;
        this.writeGate = writeGate;
        this.orgService = orgService;
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
        Optional<Human> actorOpt = orgService.findHuman(actor);
        if (actorOpt.isEmpty() || !"admin".equals(actorOpt.get().getRbac())) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(Map.of("code", "admin_only", "message", "Restore requires admin role"));
        }
        try {
            backupService.restore(body.get("backupPath"));
            return ResponseEntity.ok(Map.of("status", "restored"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "restore failed"));
        }
    }
}
