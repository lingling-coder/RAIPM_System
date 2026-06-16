package com.institute.achievement.framework.file;

import com.institute.achievement.common.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * File proxy controller (D-30).
 * <p>
 * Provides UUID-based file access without exposing the real storage path.
 * Files are served through {@code /api/files/{uuid}} URLs. The controller
 * looks up the UUID in the file_record table and serves the file from the
 * configured storage directory.
 * <p>
 * All endpoints require authentication (Spring Security default). No
 * anonymous file access is permitted.
 */
@Slf4j
@Tag(name = "文件服务", description = "文件上传与代理访问（UUID路径，不暴露存储位置）")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileProxyController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public Result<FileRecordVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") String fileType) {

        if (file.isEmpty()) {
            return Result.badRequest("文件不能为空");
        }

        try {
            byte[] content = file.getBytes();
            FileRecordVO vo = fileStorageService.store(
                    file.getOriginalFilename(),
                    content,
                    file.getSize(),
                    file.getContentType(),
                    fileType
            );
            return Result.success(vo);
        } catch (IOException e) {
            log.error("File upload failed: {}", e.getMessage());
            return Result.internalError("文件上传失败：" + e.getMessage());
        }
    }

    @Operation(summary = "下载/预览文件（UUID代理访问）")
    @GetMapping("/{uuid}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> download(@PathVariable String uuid) {
        Resource resource = fileStorageService.loadAsResource(uuid);
        FileRecordVO record = fileStorageService.getRecord(uuid);

        // Determine content type
        MediaType mediaType = resolveMediaType(record.getMimeType());

        // Build filename for Content-Disposition
        String filename = record.getOriginalName();
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(record.getFileSize() != null ? record.getFileSize() : -1)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + encodedFilename + "\"")
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS)
                        .cachePrivate()
                        .mustRevalidate())
                .body(resource);
    }

    @Operation(summary = "获取文件元信息")
    @GetMapping("/{uuid}/info")
    @PreAuthorize("isAuthenticated()")
    public Result<FileRecordVO> info(@PathVariable String uuid) {
        FileRecordVO record = fileStorageService.getRecord(uuid);
        return Result.success(record);
    }

    @Operation(summary = "删除文件")
    @PostMapping("/{uuid}/delete")
    @PreAuthorize("hasRole('ROLE_SYSTEM_ADMIN')")
    public Result<Void> delete(@PathVariable String uuid) {
        fileStorageService.delete(uuid);
        return Result.success();
    }

    // ── Private Helpers ─────────────────────────────────────────────────────

    private MediaType resolveMediaType(String mimeType) {
        if (mimeType != null && !mimeType.isEmpty()) {
            try {
                return MediaType.parseMediaType(mimeType);
            } catch (Exception e) {
                log.trace("Could not parse MIME type: {}", mimeType);
            }
        }
        // Default to octet-stream for unknown types
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
