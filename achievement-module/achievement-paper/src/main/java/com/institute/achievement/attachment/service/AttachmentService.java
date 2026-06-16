package com.institute.achievement.attachment.service;

import com.institute.achievement.attachment.entity.Attachment;
import com.institute.achievement.attachment.mapper.AttachmentMapper;
import com.institute.achievement.common.constant.AchievementConstant;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Attachment management service.
 * <p>
 * Handles file upload, download, listing, and soft-delete operations.
 * Implements D-41 (50MB limit), D-42 (no count limit), D-43 (type validation),
 * D-44 (download-only, no preview).
 * <p>
 * File storage follows the P0-06 pattern: UUID filenames, monthly bucketing,
 * and proxy-based access to avoid direct path exposure.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentMapper attachmentMapper;

    @Value("${file.storage.upload-dir:./uploads}")
    private String uploadDir;

    /**
     * Upload a file and create an attachment record.
     *
     * @param file            the uploaded file
     * @param achievementType the achievement type (paper, patent, copyright)
     * @param achievementId   the achievement ID
     * @return the attachment ID
     */
    @Transactional
    public Long upload(MultipartFile file, String achievementType, Long achievementId) {
        // D-41: Validate file size
        if (file.getSize() > AchievementConstant.MAX_FILE_SIZE) {
            throw new AchievementException(AchievementException.FILE_SIZE_EXCEEDED,
                    "文件大小超过50MB限制: " + file.getSize() + " bytes");
        }

        // D-43: Validate content type
        String contentType = file.getContentType();
        if (contentType != null && !isAllowedContentType(contentType)) {
            throw new AchievementException(AchievementException.UNSUPPORTED_FILE_TYPE,
                    "不支持的文件类型: " + contentType);
        }

        // Validate extension
        String originalName = file.getOriginalFilename();
        if (originalName != null) {
            String extension = getExtension(originalName).toLowerCase();
            boolean allowed = false;
            for (String ext : AchievementConstant.ALLOWED_EXTENSIONS) {
                if (ext.equals(extension)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                throw new AchievementException(AchievementException.UNSUPPORTED_FILE_TYPE,
                        "不支持的文件扩展名: " + extension);
            }
        }

        // Generate UUID filename (P0-06)
        String extension = originalName != null ? getExtension(originalName) : "";
        String storedName = UUID.randomUUID().toString() + extension;

        // Build storage path: {uploadDir}/{achievementType}/{yyyyMM}/{storedName}
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String relativePath = achievementType + "/" + datePath;
        String fullPath = uploadDir + "/" + relativePath;

        try {
            Path dirPath = Paths.get(fullPath);
            Files.createDirectories(dirPath);
            Path targetPath = dirPath.resolve(storedName);
            file.transferTo(targetPath.toFile());

            log.info("File stored: {} -> {}", originalName, targetPath);
        } catch (IOException e) {
            log.error("Failed to store file: {}", originalName, e);
            throw new RuntimeException("文件存储失败: " + e.getMessage(), e);
        }

        // Create attachment record with Exclusive Arc
        Attachment attachment = new Attachment();
        attachment.setOriginalName(originalName);
        attachment.setStoredName(storedName);
        attachment.setFilePath(relativePath + "/" + storedName);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(contentType != null ? contentType : "application/octet-stream");
        attachment.setUploaderId(SecurityUtils.getCurrentUserId());
        attachment.setUploadTime(LocalDateTime.now());
        attachment.setIsDeleted(0);

        // Set the appropriate FK based on achievement type
        switch (achievementType.toLowerCase()) {
            case "paper" -> attachment.setPaperId(achievementId);
            case "patent" -> attachment.setPatentId(achievementId);
            case "copyright" -> attachment.setCopyrightId(achievementId);
            default -> throw new IllegalArgumentException("Unknown achievement type: " + achievementType);
        }

        attachmentMapper.insert(attachment);
        log.info("Attachment record created: id={}, originalName={}", attachment.getId(), originalName);

        return attachment.getId();
    }

    /**
     * List attachments for a specific achievement.
     */
    public List<Attachment> getAttachments(String achievementType, Long typeId) {
        return attachmentMapper.findByOwner(achievementType, typeId);
    }

    /**
     * Get a single attachment by ID.
     */
    public Attachment getAttachmentById(Long id) {
        Attachment attachment = attachmentMapper.selectById(id);
        if (attachment == null || attachment.getIsDeleted() == 1) {
            throw AchievementException.notFound("附件", id);
        }
        return attachment;
    }

    /**
     * Soft-delete an attachment (only by uploader).
     */
    @Transactional
    public void delete(Long id, Long userId) {
        Attachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            throw AchievementException.notFound("附件", id);
        }
        if (!attachment.getUploaderId().equals(userId)) {
            throw AchievementException.notAuthorized("只能删除自己上传的附件");
        }

        attachment.setIsDeleted(1);
        attachment.setDeletedTime(LocalDateTime.now());
        attachmentMapper.updateById(attachment);
        log.info("Attachment soft-deleted: id={}, userId={}", id, userId);
    }

    // ── Internal Helpers ────────────────────────────────────────────

    private boolean isAllowedContentType(String contentType) {
        for (String allowed : AchievementConstant.ALLOWED_CONTENT_TYPES) {
            if (allowed.equals(contentType)) {
                return true;
            }
        }
        return false;
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex) : "";
    }
}
