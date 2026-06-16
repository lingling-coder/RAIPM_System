package com.institute.achievement.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for automated backup (D-32, D-33).
 * <p>
 * Maps to the {@code backup.*} prefix in application.yml.
 * Controls backup schedule, database connection, retention policy,
 * and file paths to include in backups.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "backup")
public class BackupConfig {

    /** Whether backup is enabled */
    private boolean enabled = true;

    /** Backup storage directory */
    private String backupDir = "./backups";

    /** Database connection settings */
    private DatabaseConfig database = new DatabaseConfig();

    /** Number of days to retain backup files (default 30 per D-33) */
    private int retentionDays = 30;

    /** Cron expression for scheduled backup (default 2 AM daily) */
    private String cron = "0 0 2 * * ?";

    /** List of file paths to include in backup */
    private List<String> fileBackupPaths = List.of("./uploads");

    @Data
    public static class DatabaseConfig {
        private String host = "localhost";
        private int port = 3306;
        private String username = "root";
        private String password = "root123";
        private String name = "achievement_db";
    }
}
