package com.summa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.summa.exception.EntityNotFoundException;

@Service
public class BackupService {
    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private final String dbPath;
    private final String dnaRepoPath;
    private final JdbcTemplate jdbcTemplate;

    public BackupService(
            @Value("${summa.database.path:~/.summa/summa.db}") String dbPath,
            @Value("${summa.git.dna-repo-path:~/.summa/dna}") String dnaRepoPath,
            JdbcTemplate jdbcTemplate) {
        this.dbPath = dbPath;
        this.dnaRepoPath = dnaRepoPath;
        this.jdbcTemplate = jdbcTemplate;
    }

    public String createBackup(String backupDir) throws IOException {
        Path dir = Paths.get(backupDir);
        Files.createDirectories(dir);
        
        String timestamp = Instant.now().toString().replace(":", "-");
        String backupName = "summa-backup-" + timestamp;
        Path backupPath = dir.resolve(backupName);
        Files.createDirectories(backupPath);

        // Copy database
        Path dbSrc = Paths.get(expandPath(dbPath));
        if (Files.exists(dbSrc)) {
            // Checkpoint WAL to ensure a consistent backup state
            try {
                jdbcTemplate.execute("PRAGMA wal_checkpoint(PASSIVE)");
            } catch (Exception e) {
                log.warn("WAL checkpoint failed, proceeding with best-effort backup: {}", e.getMessage(), e);
            }
            Path dbDest = backupPath.resolve("summa.db");
            Files.copy(dbSrc, dbDest, StandardCopyOption.REPLACE_EXISTING);

            // Also copy WAL if exists
            Path walSrc = Paths.get(dbSrc.toString() + "-wal");
            if (Files.exists(walSrc)) {
                Files.copy(walSrc, backupPath.resolve("summa.db-wal"), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        // Copy DNA repo
        Path dnaSrc = Paths.get(expandPath(dnaRepoPath));
        if (Files.exists(dnaSrc)) {
            Path dnaDest = backupPath.resolve("dna");
            copyDirectory(dnaSrc, dnaDest);
        }

        // Create zip
        Path zipFile = dir.resolve(backupName + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            addEntry(zos, backupPath, "summa-backup");
        }

        return zipFile.toString();
    }

    public void restore(String backupPath) throws IOException {
        Path backupFile = Paths.get(backupPath);
        if (!Files.exists(backupFile)) {
            throw new EntityNotFoundException("Backup file not found: " + backupPath);
        }

        // Prevent path traversal: resolve symlinks fully and verify within allowed dirs.
        // We follow symlinks intentionally here to detect where the path actually points.
        Path resolved;
        try {
            resolved = backupFile.toRealPath(java.nio.file.LinkOption.NOFOLLOW_LINKS);
        } catch (java.io.IOException e) {
            // toRealPath without NOFOLLOW_LINKS follows symlinks; use it as the canonical path.
            try {
                resolved = backupFile.toRealPath();
            } catch (java.io.IOException e2) {
                resolved = backupFile.toAbsolutePath().normalize();
            }
        }
        Path tmpDir;
        try {
            tmpDir = Paths.get(System.getProperty("java.io.tmpdir")).toRealPath();
        } catch (java.io.IOException e) {
            tmpDir = Paths.get(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
        }
        String dataDirStr;
        try {
            dataDirStr = Paths.get(expandPath(dbPath)).getParent().toRealPath().toString();
        } catch (java.io.IOException e) {
            dataDirStr = Paths.get(expandPath(dbPath)).getParent().toAbsolutePath().normalize().toString();
        }
        Path normalizedResolved = resolved.toAbsolutePath().normalize();
        String tmpDirStr = tmpDir.toString();
        String dataDirPrefix = dataDirStr + java.io.File.separator;
        String tmpDirPrefix = tmpDirStr + java.io.File.separator;
        if (!normalizedResolved.toString().equals(dataDirStr)
                && !normalizedResolved.toString().startsWith(dataDirPrefix)
                && !normalizedResolved.toString().equals(tmpDirStr)
                && !normalizedResolved.toString().startsWith(tmpDirPrefix)) {
            throw new IllegalArgumentException("Backup path must be under tmpdir or data directory");
        }

        Path restoreDir = Files.createTempDirectory("summa-restore-");

        try {
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(Files.newInputStream(backupFile))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path outputPath = restoreDir.resolve(entry.getName()).normalize();
                    if (!outputPath.startsWith(restoreDir)) {
                        zis.closeEntry();
                        throw new IllegalArgumentException("Zip entry escapes restore directory: " + entry.getName());
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(outputPath);
                    } else {
                        Files.copy(zis, outputPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }

            // Restore database
            Path dbSrc = restoreDir.resolve("summa-backup").resolve("summa.db");
            if (Files.exists(dbSrc)) {
                Path dbDest = Paths.get(expandPath(this.dbPath));
                Files.createDirectories(dbDest.getParent());
                Files.copy(dbSrc, dbDest, StandardCopyOption.REPLACE_EXISTING);
            }

            // Restore DNA repo
            Path dnaSrc = restoreDir.resolve("summa-backup").resolve("dna");
            if (Files.exists(dnaSrc)) {
                Path dnaDest = Paths.get(expandPath(this.dnaRepoPath));
                if (Files.exists(dnaDest)) {
                    Files.walk(dnaDest).sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> deletePathQuietly(p));
                }
                copyDirectory(dnaSrc, dnaDest);
            }
        } finally {
            deleteRestoredDir(restoreDir);
        }
    }

    private void copyDirectory(Path src, Path dest) throws IOException {
        try (java.util.stream.Stream<Path> walk = Files.walk(src)) {
            walk.forEach(source -> copyPathQuietly(source, dest, src));
        }
    }

    private void addEntry(ZipOutputStream zos, Path dir, String baseName) throws IOException {
        try (java.util.stream.Stream<Path> walk = Files.walk(dir)) {
            walk.forEach(file -> addEntryQuietly(zos, file, dir, baseName));
        }
    }

    private void deleteRestoredDir(Path restoreDir) {
        try (java.util.stream.Stream<Path> walk = Files.walk(restoreDir)) {
            walk.sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try { Files.delete(p); }
                    catch (IOException e) { log.warn("Failed to delete restored path {}: {}", p, e.getMessage()); }
                });
        } catch (IOException e) {
            log.warn("Failed to delete restored directory: {}", e.getMessage());
        }
    }

    private void copyPathQuietly(Path source, Path dest, Path root) {
        try {
            Path destination = dest.resolve(root.relativize(source));
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("Failed to copy path: {}", e.getMessage(), e);
        }
    }

    private void addEntryQuietly(ZipOutputStream zos, Path file, Path dir, String baseName) {
        try {
            String entryName = baseName + "/" + dir.relativize(file).toString().replace('\\', '/');
            if (Files.isDirectory(file)) {
                zos.putNextEntry(new ZipEntry(entryName + "/"));
            } else {
                zos.putNextEntry(new ZipEntry(entryName));
                try {
                    Files.copy(file, zos);
                } finally {
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            log.warn("Failed to add zip entry: {}", e.getMessage(), e);
        }
    }

    private void deletePathQuietly(Path p) {
        try {
            Files.delete(p);
        } catch (IOException e) {
            log.warn("Failed to delete path: {}", e.getMessage(), e);
        }
    }

    private String expandPath(String path) {
        if (path.startsWith("~")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }
}
