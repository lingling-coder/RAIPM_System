package com.institute.achievement.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for file storage (D-29, D-30, D-31).
 * <p>
 * Maps to the {@code file.storage.*} prefix in application.yml.
 * Controls upload directory, maximum file size, and allowed file types.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageConfig {

    /** Base upload directory (relative or absolute path) */
    private String uploadDir = "./uploads";

    /** Maximum file size in bytes (default 50MB per D-31) */
    private long maxFileSize = 52428800;

    /** Allowed file extensions (lowercase, without dot) */
    private List<String> allowedTypes = List.of(
            "pdf", "doc", "docx", "xls", "xlsx",
            "png", "jpg", "jpeg", "gif", "zip", "rar"
    );
}
