package com.institute.achievement.module.system.controller;

import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.module.system.dto.InvalidationDTO;
import com.institute.achievement.module.system.dto.InvalidationVO;
import com.institute.achievement.module.system.service.InvalidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for achievement invalidation operations.
 * <p>
 * Provides endpoints for invalidating archived achievements, querying
 * invalidation records, and checking visibility (D-34~D-36).
 */
@Slf4j
@RestController
@RequestMapping("/api/achievement")
@RequiredArgsConstructor
public class InvalidationController {

    private final InvalidationService invalidationService;

    /**
     * Invalidate an archived achievement.
     * POST /api/achievement/invalidate
     * <p>
     * Body: { achievementType, achievementId, reason }
     * Only creator or dept secretary can invalidate (D-34).
     */
    @PostMapping("/invalidate")
    public ResponseEntity<Map<String, Object>> invalidate(@Valid @RequestBody InvalidationDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        invalidationService.invalidate(dto, userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "成果已作废"));
    }

    /**
     * Get invalidation record for a specific achievement.
     * GET /api/achievement/invalidation?type=paper&id=1
     */
    @GetMapping("/invalidation")
    public ResponseEntity<Map<String, Object>> getInvalidationRecord(
            @RequestParam String type,
            @RequestParam Long id) {
        List<InvalidationVO> records = invalidationService.getInvalidationRecords(type, id);
        return ResponseEntity.ok(Map.of("code", 200, "data", records));
    }

    /**
     * List all invalidation records for the current user.
     * GET /api/achievement/invalidations
     * Admin sees all, others see their own.
     */
    @GetMapping("/invalidations")
    public ResponseEntity<Map<String, Object>> getInvalidations() {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.hasRole("admin");
        List<InvalidationVO> records = invalidationService.getAllInvalidations(userId, isAdmin);
        return ResponseEntity.ok(Map.of("code", 200, "data", records));
    }
}
