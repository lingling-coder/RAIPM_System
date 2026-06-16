package com.institute.achievement.attachment.controller;

import com.institute.achievement.attachment.entity.Attachment;
import com.institute.achievement.attachment.service.AttachmentService;
import com.institute.achievement.common.util.Result;
import com.institute.achievement.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * REST controller for attachment upload, download, listing, and delete.
 */
@Slf4j
@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    /**
     * Upload an attachment file.
     * POST /api/attachments/upload?type=paper&typeId=1
     */
    @PostMapping("/upload")
    public Result<Long> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String achievementType,
            @RequestParam("typeId") Long achievementId) {
        Long id = attachmentService.upload(file, achievementType, achievementId);
        return Result.success(id);
    }

    /**
     * List attachments for a specific achievement.
     * GET /api/attachments?type=paper&typeId=1
     */
    @GetMapping
    public Result<List<Attachment>> listAttachments(
            @RequestParam("type") String achievementType,
            @RequestParam("typeId") Long typeId) {
        List<Attachment> attachments = attachmentService.getAttachments(achievementType, typeId);
        return Result.success(attachments);
    }

    /**
     * Download an attachment file.
     * GET /api/attachments/{id}/download
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Attachment attachment = attachmentService.getAttachmentById(id);

        try {
            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String encodedName = URLEncoder.encode(attachment.getOriginalName(), StandardCharsets.UTF_8)
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(attachment.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedName)
                    .body(resource);

        } catch (Exception e) {
            log.error("Failed to download attachment: id={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete (soft-delete) an attachment.
     * DELETE /api/attachments/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        attachmentService.delete(id, SecurityUtils.getCurrentUserId());
        return Result.success();
    }
}
