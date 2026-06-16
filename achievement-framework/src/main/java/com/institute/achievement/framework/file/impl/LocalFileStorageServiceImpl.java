package com.institute.achievement.framework.file.impl;

import com.institute.achievement.common.exception.BusinessException;
import com.institute.achievement.common.exception.EntityNotFoundException;
import com.institute.achievement.framework.config.FileStorageConfig;
import com.institute.achievement.framework.file.FileRecordEntity;
import com.institute.achievement.framework.file.FileRecordMapper;
import com.institute.achievement.framework.file.FileRecordVO;
import com.institute.achievement.framework.file.FileStorageService;
import com.institute.achievement.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Local filesystem implementation of FileStorageService.
 * <p>
 * Files are stored in {@code {uploadDir}/YYYY/MM/type/} directories (D-29).
 * Each file gets a UUID-based filename. Access is always through the proxy
 * URL (D-30). File size is limited to 50MB (D-31).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final FileStorageConfig config;
    private final FileRecordMapper fileRecordMapper;

    private static final List<String> SENSITIVE_EXTENSIONS = List.of("exe", "bat", "cmd", "sh", "js", "vbs", "jar", "war");

    @Override
    public FileRecordVO store(String originalFilename, byte[] fileContent,
                              long fileSize, String contentType, String fileType) {
        // Validate file size (D-31)
        if (fileSize > config.getMaxFileSize()) {
            throw new BusinessException("文件大小超过限制（最大50MB）");
        }
        if (fileSize <= 0) {
            throw new BusinessException("文件不能为空");
        }

        // Extract and validate file extension
        String extension = getExtension(originalFilename);
        if (extension == null || !config.getAllowedTypes().contains(extension.toLowerCase())) {
            throw new BusinessException("不支持的文件类型：" +
                    (extension != null ? extension : "unknown") +
                    "，允许类型：" + String.join(", ", config.getAllowedTypes()));
        }

        // Block sensitive extensions for security
        if (SENSITIVE_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException("不允许上传可执行文件");
        }

        // Generate UUID filename
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String storedName = uuid + "." + extension;

        // Build storage path: YYYY/MM/type/
        LocalDate now = LocalDate.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String storageDir = datePath + "/" + fileType;

        // Create directories if needed
        Path fullDirPath = Paths.get(config.getUploadDir(), storageDir);
        try {
            Files.createDirectories(fullDirPath);
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", fullDirPath, e);
            throw new BusinessException("创建上传目录失败");
        }

        // Write file to disk
        Path targetPath = fullDirPath.resolve(storedName);
        try {
            Files.write(targetPath, fileContent);
            log.debug("File saved: {} ({} bytes)", targetPath, fileSize);
        } catch (IOException e) {
            log.error("Failed to save file: {}", targetPath, e);
            throw new BusinessException("文件保存失败");
        }

        // Create database record
        FileRecordEntity record = new FileRecordEntity();
        record.setOriginalName(originalFilename);
        record.setStoredName(uuid);
        record.setStoragePath(storageDir + "/");
        record.setFileSize(fileSize);
        record.setMimeType(contentType);
        record.setFileType(fileType);
        record.setUploadIp(SecurityUtils.getClientIp());
        record.setCreatedBy(SecurityUtils.getCurrentUserId());
        record.setCreatedAt(LocalDateTime.now());

        fileRecordMapper.insert(record);

        log.info("File stored: uuid={}, original={}, type={}, size={}",
                uuid, originalFilename, fileType, fileSize);

        // Return VO with proxy URL (D-30)
        return toVO(record);
    }

    @Override
    public Resource loadAsResource(String uuid) {
        FileRecordEntity record = fileRecordMapper.findByStoredName(uuid);
        if (record == null) {
            throw new EntityNotFoundException("文件不存在或已被删除");
        }

        try {
            Path filePath = Paths.get(config.getUploadDir(), record.getStoragePath(), uuid);
            // Try with extension
            if (!Files.exists(filePath)) {
                // Try without extension by searching directory
                Path dirPath = Paths.get(config.getUploadDir(), record.getStoragePath());
                if (Files.exists(dirPath)) {
                    try (var stream = Files.list(dirPath)) {
                        var match = stream.filter(p -> p.getFileName().toString().startsWith(uuid)).findFirst();
                        if (match.isPresent()) {
                            filePath = match.get();
                        } else {
                            throw new EntityNotFoundException("文件不存在或已被删除");
                        }
                    }
                } else {
                    throw new EntityNotFoundException("文件不存在或已被删除");
                }
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new EntityNotFoundException("文件不存在或无法读取");
            }

            return resource;

        } catch (IOException e) {
            log.error("Failed to load file with uuid: {}", uuid, e);
            throw new BusinessException("文件读取失败");
        }
    }

    @Override
    public FileRecordVO getRecord(String uuid) {
        FileRecordEntity record = fileRecordMapper.findByStoredName(uuid);
        if (record == null) {
            throw new EntityNotFoundException("文件不存在");
        }
        return toVO(record);
    }

    @Override
    public void delete(String uuid) {
        FileRecordEntity record = fileRecordMapper.findByStoredName(uuid);
        if (record == null) {
            throw new EntityNotFoundException("文件不存在");
        }

        // Delete physical file
        try {
            Path dirPath = Paths.get(config.getUploadDir(), record.getStoragePath());
            try (var stream = Files.list(dirPath)) {
                stream.filter(p -> p.getFileName().toString().startsWith(record.getStoredName()))
                        .findFirst()
                        .ifPresent(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                log.warn("Failed to delete physical file: {}", p, e);
                            }
                        });
            }
        } catch (IOException e) {
            log.warn("Failed to list files for deletion: uuid={}", uuid, e);
        }

        // Delete database record
        fileRecordMapper.deleteById(record.getId());
        log.info("File deleted: uuid={}, original={}", uuid, record.getOriginalName());
    }

    // ── Private Helpers ─────────────────────────────────────────────────────

    private FileRecordVO toVO(FileRecordEntity entity) {
        FileRecordVO vo = new FileRecordVO();
        vo.setId(entity.getId());
        vo.setOriginalName(entity.getOriginalName());
        vo.setStoredName(entity.getStoredName());
        vo.setFileSize(entity.getFileSize());
        vo.setMimeType(entity.getMimeType());
        vo.setFileType(entity.getFileType());
        vo.setProxyUrl("/api/files/" + entity.getStoredName());
        vo.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        return vo;
    }

    /**
     * Extract file extension from original filename.
     */
    private String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot >= filename.length() - 1) {
            return null;
        }
        return filename.substring(lastDot + 1).toLowerCase();
    }
}
