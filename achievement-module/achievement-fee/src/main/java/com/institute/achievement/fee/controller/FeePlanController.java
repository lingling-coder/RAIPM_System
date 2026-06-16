package com.institute.achievement.fee.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.util.Result;
import com.institute.achievement.fee.dto.FeePlanDTO;
import com.institute.achievement.fee.dto.FeePlanQueryDTO;
import com.institute.achievement.fee.dto.FeePlanVO;
import com.institute.achievement.fee.service.FeePlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for fee plan CRUD and paginated listing.
 * <p>
 * All endpoints return {@link Result}<T> wrapper consistent with the
 * project-wide API response pattern.
 *
 * <h3>Security mitigations</h3>
 * <ul>
 *   <li>T-02-02-01: Service only copies amount, fundingSource from DTO — ignores dueDate, patentId</li>
 *   <li>T-02-02-02: Java-level dedup + UNIQUE INDEX prevents duplicate plans</li>
 *   <li>T-02-02-04: SQL-layer dept_id injection via MyBatis-Plus interceptor</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/fee-plans")
@RequiredArgsConstructor
public class FeePlanController {

    private final FeePlanService feePlanService;

    /**
     * Create a new fee plan (manual one-time plan).
     */
    @PostMapping
    public Result<Long> createPlan(@Valid @RequestBody FeePlanDTO dto) {
        Long id = feePlanService.create(dto);
        return Result.success(id);
    }

    /**
     * Update an existing fee plan.
     * <p>
     * Only whitelisted fields are mutable: amount, fundingSource.
     * dueDate and patentId are system-locked (D-17).
     */
    @PutMapping("/{id}")
    public Result<Void> updatePlan(@PathVariable Long id, @Valid @RequestBody FeePlanDTO dto) {
        feePlanService.update(id, dto);
        return Result.success();
    }

    /**
     * Get a fee plan by ID.
     */
    @GetMapping("/{id}")
    public Result<FeePlanVO> getPlan(@PathVariable Long id) {
        FeePlanVO vo = feePlanService.getById(id);
        return Result.success(vo);
    }

    /**
     * Delete a fee plan (only allowed for paused plans).
     */
    @DeleteMapping("/{id}")
    public Result<Void> deletePlan(@PathVariable Long id) {
        feePlanService.delete(id);
        return Result.success();
    }

    /**
     * Pause a fee plan.
     */
    @PutMapping("/{id}/pause")
    public Result<Void> pausePlan(@PathVariable Long id) {
        feePlanService.pause(id);
        return Result.success();
    }

    /**
     * Restore a paused fee plan.
     */
    @PutMapping("/{id}/restore")
    public Result<Void> restorePlan(@PathVariable Long id) {
        feePlanService.restore(id);
        return Result.success();
    }

    /**
     * Paginated fee plan listing with multi-dimensional filtering.
     *
     * @param page     page number (1-based, default 1)
     * @param size     page size (default 20)
     * @param status   filter by plan status (active/paused)
     * @param feeType  filter by fee type code
     * @param keyword  search keyword (matches patent name)
     * @param patentId filter by patent ID
     * @return paginated fee plan list with patent info
     */
    @GetMapping("/page")
    public Result<Page<FeePlanVO>> pagePlans(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String feeType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long patentId) {

        FeePlanQueryDTO query = new FeePlanQueryDTO();
        query.setStatus(status);
        query.setFeeType(feeType);
        query.setKeyword(keyword);
        query.setPatentId(patentId);

        Page<FeePlanVO> result = feePlanService.page(page, size, query);
        return Result.success(result);
    }
}
