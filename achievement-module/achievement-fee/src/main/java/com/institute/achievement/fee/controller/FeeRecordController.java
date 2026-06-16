package com.institute.achievement.fee.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.util.Result;
import com.institute.achievement.fee.dto.FeeRecordDTO;
import com.institute.achievement.fee.dto.FeeRecordQueryDTO;
import com.institute.achievement.fee.dto.FeeRecordVO;
import com.institute.achievement.fee.service.FeeRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for fee record CRUD and paginated listing.
 * <p>
 * All endpoints return {@link Result}<T> wrapper consistent with the
 * project-wide API response pattern.
 *
 * <h3>Security mitigations</h3>
 * <ul>
 *   <li>T-02-01-01: creator/updater injected from SecurityUtils in service layer</li>
 *   <li>T-02-01-02: UpdateWrapper whitelist in service (only editable fields)</li>
 *   <li>T-02-01-03: SQL-layer dept_id injection via MyBatis-Plus interceptor</li>
 *   <li>T-02-01-04: Paginated queries limit result set size</li>
 *   <li>T-02-01-05: Delete restricted to creator + paused status in service</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/fees")
@RequiredArgsConstructor
public class FeeRecordController {

    private final FeeRecordService feeRecordService;

    /**
     * Create a new fee record.
     */
    @PostMapping
    public Result<Long> createFeeRecord(@Valid @RequestBody FeeRecordDTO dto) {
        Long id = feeRecordService.create(dto);
        return Result.success(id);
    }

    /**
     * Update an existing fee record.
     * <p>
     * Only whitelisted fields are mutable: amount, fundingSource,
     * paidAmount, voucherNo, status. dueDate/ownerType/ownerId are
     * system-locked (D-17).
     */
    @PutMapping("/{id}")
    public Result<Void> updateFeeRecord(@PathVariable Long id, @Valid @RequestBody FeeRecordDTO dto) {
        feeRecordService.update(id, dto);
        return Result.success();
    }

    /**
     * Get a fee record by ID.
     */
    @GetMapping("/{id}")
    public Result<FeeRecordVO> getFeeRecord(@PathVariable Long id) {
        FeeRecordVO vo = feeRecordService.getById(id);
        return Result.success(vo);
    }

    /**
     * Delete a fee record.
     * <p>
     * Only allowed for paused records and by the original creator (T-02-01-05).
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteFeeRecord(@PathVariable Long id) {
        feeRecordService.delete(id);
        return Result.success();
    }

    /**
     * Paginated fee record listing with multi-dimensional filtering.
     *
     * @param page          page number (1-based, default 1)
     * @param size          page size (default 20)
     * @param status        filter by fee status code
     * @param feeType       filter by fee type code
     * @param fundingSource filter by funding source code
     * @param keyword       search keyword (matches patent/copyright name)
     * @param dueDateFrom   due date range start (inclusive), format yyyy-MM-dd
     * @param dueDateTo     due date range end (inclusive), format yyyy-MM-dd
     * @param ownerType     filter by owner type (patent/copyright)
     * @return paginated fee record list with owner names
     */
    @GetMapping("/page")
    public Result<Page<FeeRecordVO>> pageFeeRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String feeType,
            @RequestParam(required = false) String fundingSource,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dueDateFrom,
            @RequestParam(required = false) String dueDateTo,
            @RequestParam(required = false) String ownerType) {

        FeeRecordQueryDTO query = new FeeRecordQueryDTO();
        query.setStatus(status);
        query.setFeeType(feeType);
        query.setFundingSource(fundingSource);
        query.setKeyword(keyword);
        query.setDueDateFrom(dueDateFrom);
        query.setDueDateTo(dueDateTo);
        query.setOwnerType(ownerType);

        Page<FeeRecordVO> result = feeRecordService.page(page, size, query);
        return Result.success(result);
    }
}
