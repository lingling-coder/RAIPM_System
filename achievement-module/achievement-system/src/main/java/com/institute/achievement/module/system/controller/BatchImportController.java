package com.institute.achievement.module.system.controller;

import com.institute.achievement.common.util.Result;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.module.system.dto.BatchImportResult;
import com.institute.achievement.module.system.service.BatchImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * REST controller for batch import operations.
 * <p>
 * Provides endpoints for importing Excel files, downloading the import template,
 * and downloading error reports for specific import operations.
 * <p>
 * Implements D-16 (direct import), D-18 (partial import), D-21 (template download).
 */
@Slf4j
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchImportController {

    private final BatchImportService batchImportService;

    /**
     * Upload and import an Excel file.
     * POST /api/batch/import
     */
    @PostMapping("/import")
    public Result<BatchImportResult> importFile(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("未登录");
        }
        BatchImportResult result = batchImportService.importExcel(file, userId);
        return Result.success(result);
    }

    /**
     * Download the blank import template Excel file.
     * GET /api/batch/template
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] templateBytes = batchImportService.downloadTemplate();

        String filename = URLEncoder.encode("科研成果导入模板.xlsx",
                StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .body(templateBytes);
    }

    /**
     * Download error report for a specific import operation.
     * GET /api/batch/error-report/{importRecordId}
     */
    @GetMapping("/error-report/{importRecordId}")
    public ResponseEntity<byte[]> downloadErrorReport(@PathVariable Long importRecordId) {
        // For Phase 1: error report generation simplified
        return ResponseEntity.notFound().build();
    }

    /**
     * List import history for the current user.
     * GET /api/batch/records
     */
    @GetMapping("/records")
    public Result<?> getImportRecords() {
        // For Phase 1: returns empty list (simplified)
        return Result.success(java.util.Collections.emptyList());
    }
}
