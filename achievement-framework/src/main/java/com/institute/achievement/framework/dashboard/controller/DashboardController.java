package com.institute.achievement.framework.dashboard.controller;

import com.institute.achievement.common.util.Result;
import com.institute.achievement.framework.dashboard.dto.DashboardDeptRankVO;
import com.institute.achievement.framework.dashboard.dto.DashboardPatentStatusVO;
import com.institute.achievement.framework.dashboard.dto.DashboardTrendVO;
import com.institute.achievement.framework.dashboard.dto.DashboardTypeDistVO;
import com.institute.achievement.framework.dashboard.service.DashboardService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for dashboard statistics aggregation and export.
 * <p>
 * Provides 4 chart data endpoints, Excel export, and PDF export.
 * All chart data endpoints return {@link Result}<T> wrapper consistent
 * with the project-wide API response pattern.
 *
 * <h3>Security mitigations</h3>
 * <ul>
 *   <li>T-3-01-01: chartType validated via service-layer whitelist in export methods</li>
 *   <li>T-3-01-02: SQL-layer dept_id injection via @DataScope on DashboardMapper</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get annual trend of achievement counts grouped by year and type.
     *
     * @param deptId   optional department filter
     * @param yearFrom optional start year filter
     * @param yearTo   optional end year filter
     * @return list of trend data points
     */
    @GetMapping("/annual-trend")
    public Result<List<DashboardTrendVO>> getAnnualTrend(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo) {
        List<DashboardTrendVO> trend = dashboardService.getAnnualTrend(deptId, yearFrom, yearTo);
        return Result.success(trend);
    }

    /**
     * Get type distribution of all achievements (paper / patent / software).
     *
     * @param deptId optional department filter
     * @return list of type distribution data points with percentages
     */
    @GetMapping("/type-dist")
    public Result<List<DashboardTypeDistVO>> getTypeDist(
            @RequestParam(required = false) Long deptId) {
        List<DashboardTypeDistVO> dist = dashboardService.getTypeDist(deptId);
        return Result.success(dist);
    }

    /**
     * Get department ranking by total achievement count.
     *
     * @param deptId optional department filter
     * @return list of department ranking data points
     */
    @GetMapping("/dept-ranking")
    public Result<List<DashboardDeptRankVO>> getDeptRanking(
            @RequestParam(required = false) Long deptId) {
        List<DashboardDeptRankVO> ranking = dashboardService.getDeptRanking(deptId);
        return Result.success(ranking);
    }

    /**
     * Get patent legal status distribution (VALID / INVALID / UNKNOWN).
     *
     * @param deptId optional department filter
     * @return list of patent status data points with percentages
     */
    @GetMapping("/patent-status")
    public Result<List<DashboardPatentStatusVO>> getPatentStatus(
            @RequestParam(required = false) Long deptId) {
        List<DashboardPatentStatusVO> status = dashboardService.getPatentStatus(deptId);
        return Result.success(status);
    }

    /**
     * Export chart detail data to Excel via EasyExcel streaming.
     *
     * @param chartType the chart type key (validated against whitelist)
     * @param deptId    optional department filter
     * @param response  the HTTP response for writing the Excel stream
     */
    @GetMapping("/export/{chartType}")
    public void exportExcel(
            @PathVariable String chartType,
            @RequestParam(required = false) Long deptId,
            HttpServletResponse response) {
        dashboardService.exportExcel(response, chartType, deptId);
    }

    /**
     * Export chart report to PDF via iText 7.
     *
     * @param chartType the chart type key (validated against whitelist)
     * @param deptId    optional department filter
     * @param response  the HTTP response for writing the PDF stream
     */
    @GetMapping("/export-pdf/{chartType}")
    public void exportPdf(
            @PathVariable String chartType,
            @RequestParam(required = false) Long deptId,
            HttpServletResponse response) {
        dashboardService.exportPdf(response, chartType, deptId);
    }
}
