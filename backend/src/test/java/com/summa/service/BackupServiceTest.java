package com.summa.service;

import com.summa.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BackupServiceTest {

    @TempDir
    Path tempDir;

    private BackupService backupService;
    private Path dbPath;
    private Path dnaPath;
    private Path backupDir;

    @BeforeEach
    void setUp() throws IOException {
        dbPath = tempDir.resolve("summa.db");
        dnaPath = tempDir.resolve("dna");
        backupDir = tempDir.resolve("backups");
        Files.createDirectories(dnaPath);
        Files.writeString(dbPath, "test database content");
        Files.writeString(dnaPath.resolve("domain.md"), "# Test Domain");
        Files.createDirectories(backupDir);

        backupService = new BackupService(
            dbPath.toString(),
            dnaPath.toString()
        );
    }

    @Test
    void createBackup_copiesDbAndDna() throws IOException {
        String zipPath = backupService.createBackup(backupDir.toString());

        Path zipFile = Path.of(zipPath);
        assertTrue(Files.exists(zipFile));
        assertTrue(zipFile.toString().endsWith(".zip"));
    }

    @Test
    void createBackup_handlesMissingDbGracefully() throws IOException {
        Path emptyBackupDir = tempDir.resolve("empty-backups");
        Files.createDirectories(emptyBackupDir);
        BackupService service = new BackupService(
            tempDir.resolve("nonexistent.db").toString(),
            dnaPath.toString()
        );

        String zipPath = service.createBackup(emptyBackupDir.toString());
        assertTrue(Files.exists(Path.of(zipPath)));
    }

    @Test
    void restore_validBackupRestoresFiles() throws IOException {
        String zipPath = backupService.createBackup(backupDir.toString());

        Path restoreTarget = tempDir.resolve("restore");
        Files.createDirectories(restoreTarget);
        // Change the db content before restore
        Files.writeString(dbPath, "modified content");

        backupService.restore(zipPath);

        String restoredDb = Files.readString(dbPath);
        assertEquals("test database content", restoredDb);
        String restoredDna = Files.readString(dnaPath.resolve("domain.md"));
        assertEquals("# Test Domain", restoredDna);
    }

    @Test
    void restore_throwsWhenFileNotFound() {
        assertThrows(EntityNotFoundException.class, () ->
            backupService.restore(tempDir.resolve("nonexistent.zip").toString())
        );
    }

    @Test
    void restore_rejectsPathOutsideAllowedDirs() throws IOException {
        // Create a zip at a path clearly outside /tmp and tempDir
        Path outsideDir = Path.of(System.getProperty("user.home"), "summa-test-outside");
        Files.createDirectories(outsideDir);
        Path evilZip = outsideDir.resolve("evil.zip");
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(evilZip))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("summa-backup/summa.db"));
            zos.write("evil".getBytes());
            zos.closeEntry();
        }

        assertThrows(IllegalArgumentException.class, () ->
            backupService.restore(evilZip.toString())
        );

        // Cleanup
        Files.walk(outsideDir)
            .sorted((a, b) -> b.compareTo(a))
            .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
    }

    @Test
    void restore_rejectsZipWithEscapingEntries() throws IOException {
        Path evilDir = tempDir.resolve("evil-zip-src");
        Files.createDirectories(evilDir);
        Path evilZip = tempDir.resolve("evil-escaping.zip");
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(evilZip))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("summa-backup/../../../etc/evil.txt"));
            zos.write("bad".getBytes());
            zos.closeEntry();
        }

        assertThrows(IllegalArgumentException.class, () ->
            backupService.restore(evilZip.toString())
        );
    }

    @Test
    void restore_preservesDataDirParentEvenIfNotExists() throws IOException {
        Path newDbPath = tempDir.resolve("new-db-dir").resolve("summa.db");
        Path newDnaPath = tempDir.resolve("new-dna-dir");
        BackupService service = new BackupService(
            newDbPath.toString(),
            newDnaPath.toString()
        );

        // Create a backup first using the original service
        String zipPath = backupService.createBackup(backupDir.toString());

        // Restore into the new paths — service should create parent dirs
        service.restore(zipPath);

        assertTrue(Files.exists(newDbPath.getParent()));
    }
}
