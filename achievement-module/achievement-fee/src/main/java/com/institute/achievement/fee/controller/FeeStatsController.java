package com.institute.achievement.fee.controller;

import com.institute.achievement.common.util.Result;
import com.institute.achievement.fee.dto.FeeStatsVO;
import com.institute.achievement.fee.service.FeeStatsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for fee statistics aggregation and export.
 * <p>
 * Provides overview summary, multi-dimensional grouped statistics (D-28),
 * and EasyExcel export (D-29). All endpoints return {@link Result}<T> wrapper
 * consistent with the project-wide API response pattern.
 *
 * <h3>Security mitigations</h3>
 * <ul>
 *   <li>T-02-05-01: SQL-layer dept_id injection via MyBatis-Plus interceptor</li>
 *   <li>T-02-05-02: Dimension whitelist validation in service layer</li>
 *   <li>T-02-05-03: Composite index idx_fee_record_stats for query performance</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/fees/stats")
@RequiredArgsConstructor
public class FeeStatsController {

    private final FeeStatsService feeStatsService;

    /**
     * Get overview summary of fee records.
     * <p>
     * Returns 4 metric values: totalAmount, totalPaid, totalPending, totalOverdue, recordCount.
     * All BigDecimal values default to 0 when no records match.
     *
     * @param deptId        filter by department ID (optional)
     * @param year          filter by due date year (optional)
     * @param patentType    filter by patent type code (optional)
     * @param fundingSource filter by funding source code (optional)
     * @return overview summary
     */
    @GetMapping("/overview")
    public Result<FeeStatsVO> getOverview(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String patentType,
            @RequestParam(required = false) String fundingSource) {
        FeeStatsVO overview = feeStatsService.getOverview(deptId, year, patentType, fundingSource);
        return Result.success(overview);
    }

    /**
     * Get fee statistics grouped by a dimension.
     * <p>
     * Valid dimensions (D-28): dept_id, YEAR(due_date), patent_type, funding_source.
     * Invalid dimensions return 400 error (via IllegalArgumentException handler).
     *
     * @param dimension     the grouping dimension (required, whitelist-validated)
     * @param deptId        cross-filter by department ID (optional)
     * @param year          cross-filter by due date year (optional)
     * @param patentType    cross-filter by patent type code (optional)
     * @param fundingSource cross-filter by funding source code (optional)
     * @return list of dimension rows with aggregate values
     */
    @GetMapping("/dimension")
    public Result<List<FeeStatsVO>> getDimensionStats(
            @RequestParam String dimension,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String patentType,
            @RequestParam(required = false) String fundingSource) {
        List<FeeStatsVO> stats = feeStatsService.getDimensionStats(dimension, deptId, year, patentType, fundingSource);
        return Result.success(stats);
    }

    /**
     * Export fee statistics to Excel.
     * <p>
     * Streams an Excel file via EasyExcel with Content-Disposition attachment.
     * The exported data is grouped by department dimension with current filters
     * applied. On query failure, returns a JSON error response.
     *
     * @param response      the HTTP response (used for writing Excel stream)
     * @param deptId        cross-filter by department ID (optional)
     * @param year          cross-filter by due date year (optional)
     * @param patentType    cross-filter by patent type code (optional)
     * @param fundingSource cross-filter by funding source code (optional)
     */
    @GetMapping("/export")
    public void exportExcel(
            HttpServletResponse response,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String patentType,
            @RequestParam(required = false) String fundingSource) {
        feeStatsService.exportExcel(response, deptId, year, patentType, fundingSource);
    }
}
