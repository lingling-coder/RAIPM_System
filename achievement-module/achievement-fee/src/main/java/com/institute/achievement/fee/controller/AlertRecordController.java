package com.institute.achievement.fee.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.util.Result;
import com.institute.achievement.fee.dto.AlertQueryDTO;
import com.institute.achievement.fee.dto.AlertRecordVO;
import com.institute.achievement.fee.service.AlertRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for alert record query and management.
 * <p>
 * Provides paginated listing, single record detail, and resolve operations.
 * All endpoints return {@link Result}<T> wrapper consistent with the
 * project-wide API response pattern.
 *
 * <h3>Security mitigations</h3>
 * <ul>
 *   <li>T-02-03-03: SQL-layer dept_id injection via MyBatis-Plus interceptor</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/alert-records")
@RequiredArgsConstructor
public class AlertRecordController {

    private final AlertRecordService alertRecordService;

    /**
     * Paginated alert record listing with optional filtering.
     *
     * @param page       page number (1-based, default 1)
     * @param size       page size (default 20)
     * @param status     filter by alert status (pending/resolved/ignored)
     * @param alertLevel filter by alert level (BLUE/YELLOW/ORANGE/RED)
     * @return paginated alert record list with JOIN fields
     */
    @GetMapping("/page")
    public Result<Page<AlertRecordVO>> pageAlertRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String alertLevel) {

        AlertQueryDTO query = new AlertQueryDTO();
        query.setStatus(status);
        query.setAlertLevel(alertLevel);

        Page<AlertRecordVO> result = alertRecordService.page(page, size, query);
        return Result.success(result);
    }

    /**
     * Get an alert record by ID.
     *
     * @param id the alert record ID
     * @return the alert record with full details
     */
    @GetMapping("/{id}")
    public Result<AlertRecordVO> getAlertRecord(@PathVariable Long id) {
        AlertRecordVO vo = alertRecordService.getById(id);
        return Result.success(vo);
    }

    /**
     * Resolve a single alert.
     *
     * @param id the alert record ID to resolve
     * @return success response
     */
    @PutMapping("/{id}/resolve")
    public Result<Void> resolveAlert(@PathVariable Long id) {
        alertRecordService.resolve(id);
        return Result.success();
    }

    /**
     * Batch resolve multiple alerts.
     *
     * @param ids list of alert record IDs to resolve
     * @return success response
     */
    @PutMapping("/batch-resolve")
    public Result<Void> batchResolveAlerts(@RequestBody List<Long> ids) {
        alertRecordService.batchResolve(ids);
        return Result.success();
    }

    /**
     * Manually trigger escalation for a specific alert record.
     * <p>
     * Evaluates the alert's age and escalates to DEPT_HEAD or LEADERSHIP
     * as appropriate. Sends escalation notifications to the correct
     * RBAC-routed users.
     *
     * @param id the alert record ID to escalate
     * @return success response
     */
    @PostMapping("/{id}/escalate")
    public Result<Void> escalateAlert(@PathVariable Long id) {
        alertRecordService.processSingleEscalation(id);
        return Result.success();
    }
}
