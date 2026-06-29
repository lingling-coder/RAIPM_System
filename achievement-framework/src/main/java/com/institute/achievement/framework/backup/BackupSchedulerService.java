package com.institute.achievement.framework.backup;

import com.institute.achievement.framework.config.BackupConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Automated backup scheduler (D-32, D-33).
 * <p>
 * Performs daily full backup of database (mysqldump) and file storage directory.
 * Backup files are retained for 30 days (configurable). Older backups are
 * automatically cleaned up.
 * <p>
 * MySQL dump: uses ProcessBuilder to execute mysqldump
 * File backup: uses tar on Linux/macOS, ZipOutputStream on Windows
 * Retention: deletes backup files/directories older than retentionDays
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupSchedulerService {

    private final BackupConfig config;

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @PostConstruct
    public void init() {
        if (config.isEnabled()) {
            log.info("Backup scheduler initialized: cron={}, retention={} days, backupDir={}",
                    config.getCron(), config.getRetentionDays(), config.getBackupDir());
            // Ensure backup directory exists
            try {
                Files.createDirectories(Paths.get(config.getBackupDir()));
            } catch (IOException e) {
                log.warn("Could not create backup directory: {}", config.getBackupDir(), e);
            }
        }
    }

    /**
     * Scheduled backup task. Runs at the configured cron time (default 2 AM daily).
     * <p>
     * Steps:
     * 1. Database backup via mysqldump
     * 2. File storage backup (tar on Linux, zip on Windows)
     * 3. Retention cleanup (remove files older than retentionDays)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void backup() {
        if (!config.isEnabled()) {
            log.debug("Backup is disabled, skipping scheduled backup");
            return;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        log.info("Starting scheduled backup at {}", timestamp);

        Path backupDir = Paths.get(config.getBackupDir());
        boolean hasError = false;

        try {
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            log.error("Cannot create backup directory: {}", backupDir, e);
            return;
        }

        // Step 1: Database backup via mysqldump
        try {
            backupDatabase(backupDir, timestamp);
        } catch (Exception e) {
            log.error("Database backup failed", e);
            hasError = true;
        }

        // Step 2: File storage backup
        try {
            backupFiles(backupDir, timestamp);
        } catch (Exception e) {
            log.error("File backup failed", e);
            hasError = true;
        }

        // Step 3: Retention cleanup
        try {
            cleanupOldBackups(backupDir);
        } catch (Exception e) {
            log.error("Backup retention cleanup failed", e);
            hasError = true;
        }

        if (hasError) {
            log.warn("Backup completed with errors (timestamp: {})", timestamp);
        } else {
            log.info("Backup completed successfully (timestamp: {})", timestamp);
        }
    }

    /**
     * Database backup using mysqldump.
     * Executes an external process: mysqldump -h{host} -P{port} -u{user} -p{pass} {dbname} > {file}
     */
    private void backupDatabase(Path backupDir, String timestamp) throws Exception {
        String dumpFile = backupDir.resolve("achievement-db-" + timestamp + ".sql").toString();
        BackupConfig.DatabaseConfig db = config.getDatabase();

        ProcessBuilder pb = new ProcessBuilder(
                "mysqldump",
                "-h" + db.getHost(),
                "-P" + String.valueOf(db.getPort()),
                "-u" + db.getUsername(),
                "-p" + db.getPassword(),
                db.getName(),
                "--routines",
                "--triggers",
                "--single-transaction"
        );

        pb.redirectOutput(new File(dumpFile));
        pb.redirectErrorStream(true);

        log.info("Starting database backup to: {}", dumpFile);
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            long fileSize = Files.size(Paths.get(dumpFile));
            log.info("Database backup completed: {} ({} bytes)", dumpFile, fileSize);
        } else {
            log.warn("mysqldump exited with code: {}", exitCode);
            // Read error output
            String errorOutput = new String(process.getInputStream().readAllBytes());
            log.warn("mysqldump stderr: {}", errorOutput);
        }
    }

    /**
     * File backup using ZipOutputStream on Windows (no tar dependency).
     * On Linux/Mac, uses tar command if available; falls back to Zip.
     */
    private void backupFiles(Path backupDir, String timestamp) throws IOException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

        if (isWindows) {
            // Windows: use Java ZipOutputStream
            backupFilesWithZip(backupDir, timestamp);
        } else {
            // Try tar first, fall back to zip
            try {
                backupFilesWithTar(backupDir, timestamp);
            } catch (Exception e) {
                log.warn("tar backup failed, falling back to zip: {}", e.getMessage());
                backupFilesWithZip(backupDir, timestamp);
            }
        }
    }

    /**
     * Backup files using tar command (Linux/macOS).
     */
    private void backupFilesWithTar(Path backupDir, String timestamp) throws Exception {
        String tarFile = backupDir.resolve("uploads-" + timestamp + ".tar.gz").toString();

        for (String path : config.getFileBackupPaths()) {
            Path sourcePath = Paths.get(path);
            if (!Files.exists(sourcePath)) {
                log.warn("File backup path does not exist, skipping: {}", path);
                continue;
            }

            ProcessBuilder pb = new ProcessBuilder(
                    "tar", "-czf", tarFile, "-C",
                    sourcePath.getParent().toString(),
                    sourcePath.getFileName().toString()
            );
            pb.redirectErrorStream(true);

            log.info("Starting file backup (tar) from: {} to: {}", path, tarFile);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                long fileSize = Files.size(Paths.get(tarFile));
                log.info("File backup (tar) completed: {} ({} bytes)", tarFile, fileSize);
            } else {
                String errorOutput = new String(process.getInputStream().readAllBytes());
                log.warn("tar backup exit code: {}, stderr: {}", exitCode, errorOutput);
                throw new IOException("tar backup failed with exit code " + exitCode);
            }
        }
    }

    /**
     * Backup files using Java ZipOutputStream (cross-platform).
     */
    private void backupFilesWithZip(Path backupDir, String timestamp) throws IOException {
        String zipFile = backupDir.resolve("uploads-" + timestamp + ".zip").toString();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (String path : config.getFileBackupPaths()) {
                Path sourcePath = Paths.get(path);
                if (!Files.exists(sourcePath)) {
                    log.warn("File backup path does not exist, skipping: {}", path);
                    continue;
                }

                Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        // Create zip entry with relative path
                        String entryName = sourcePath.getParent().relativize(file).toString()
                                .replace("\\", "/");
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(file, zos);
                        zos.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        // Add directory entries
                        String entryName = sourcePath.getParent().relativize(dir).toString()
                                .replace("\\", "/") + "/";
                        zos.putNextEntry(new ZipEntry(entryName));
                        zos.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }

        long fileSize = Files.size(Paths.get(zipFile));
        log.info("File backup (zip) completed: {} ({} bytes)", zipFile, fileSize);
    }

    /**
     * Clean up backup files older than the configured retention period.
     * Checks backup directory for files/directories older than retentionDays.
     */
    private void cleanupOldBackups(Path backupDir) {
        if (config.getRetentionDays() <= 0) {
            return;
        }

        long cutoff = System.currentTimeMillis() - ((long) config.getRetentionDays() * 24 * 60 * 60 * 1000L);
        log.debug("Cleaning backup files older than {} days (cutoff: {})",
                config.getRetentionDays(), cutoff);

        File[] backupFiles = backupDir.toFile().listFiles();
        if (backupFiles == null) {
            return;
        }

        int deletedCount = 0;
        for (File file : backupFiles) {
            if (file.lastModified() < cutoff) {
                boolean deleted = deleteFileOrDirectory(file.toPath());
                if (deleted) {
                    deletedCount++;
                    log.debug("Deleted old backup: {}", file.getName());
                }
            }
        }

        if (deletedCount > 0) {
            log.info("Backup retention cleanup: deleted {} old backup(s)", deletedCount);
        }
    }

    /**
     * Recursively delete a file or directory.
     */
    private boolean deleteFileOrDirectory(Path path) {
        try {
            if (Files.isDirectory(path)) {
                Files.walkFileTree(path, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                Files.delete(path);
            }
            return true;
        } catch (IOException e) {
            log.warn("Failed to delete old backup: {}", path, e);
            return false;
        }
    }
}
