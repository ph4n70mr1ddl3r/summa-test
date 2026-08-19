package com.summa.service;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {
    private final String dbPath;
    private final String dnaRepoPath;

    public BackupService(
            @org.springframework.beans.factory.annotation.Value("${summa.database.path:~/.summa/summa.db}") String dbPath,
            @org.springframework.beans.factory.annotation.Value("${summa.git.dna-repo-path:~/.summa/dna}") String dnaRepoPath) {
        this.dbPath = dbPath;
        this.dnaRepoPath = dnaRepoPath;
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
            throw new IllegalArgumentException("Backup file not found: " + backupPath);
        }

        Path restoreDir = Files.createTempDirectory("summa-restore-");
        
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(Files.newInputStream(backupFile))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outputPath = restoreDir.resolve(entry.getName());
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

    private void copyPathQuietly(Path source, Path dest, Path root) {
        try {
            Path destination = dest.resolve(root.relativize(source));
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addEntryQuietly(ZipOutputStream zos, Path file, Path dir, String baseName) {
        try {
            String entryName = baseName + "/" + dir.relativize(file).toString().replace('\\', '/');
            if (Files.isDirectory(file)) {
                zos.putNextEntry(new ZipEntry(entryName + "/"));
            } else {
                zos.putNextEntry(new ZipEntry(entryName));
                Files.copy(file, zos);
            }
            zos.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deletePathQuietly(Path p) {
        try {
            Files.delete(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String expandPath(String path) {
        if (path.startsWith("~")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }
}
