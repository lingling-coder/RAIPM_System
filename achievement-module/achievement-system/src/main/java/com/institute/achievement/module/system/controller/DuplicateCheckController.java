package com.institute.achievement.module.system.controller;

import com.institute.achievement.module.system.dto.DuplicateCheckResult;
import com.institute.achievement.module.system.service.DuplicateCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for submit-time duplicate detection.
 * <p>
 * Provides an endpoint for the frontend DuplicateDialog to check
 * whether a DOI/applicationNo/registrationNo is already registered.
 * <p>
 * Implements D-45~D-47: submit-time check, dialog display, draft skip.
 */
@Slf4j
@RestController
@RequestMapping("/api/achievement")
@RequiredArgsConstructor
public class DuplicateCheckController {

    private final DuplicateCheckService duplicateCheckService;

    /**
     * Check for duplicate achievement at submit time.
     * GET /api/achievement/check-duplicate?type=paper&field=10.1234/test
     * <p>
     * Returns DuplicateCheckResult with existing achievement info if duplicate found.
     */
    @GetMapping("/check-duplicate")
    public ResponseEntity<Map<String, Object>> checkDuplicate(
            @RequestParam String type,
            @RequestParam String field) {
        DuplicateCheckResult result = duplicateCheckService.checkDuplicateForSubmit(type, field);
        return ResponseEntity.ok(Map.of("code", 200, "data", result));
    }
}
