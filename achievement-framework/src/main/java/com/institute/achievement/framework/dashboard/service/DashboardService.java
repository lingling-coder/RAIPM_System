package com.institute.achievement.framework.dashboard.service;

import com.institute.achievement.framework.dashboard.dto.DashboardDeptRankVO;
import com.institute.achievement.framework.dashboard.dto.DashboardPatentStatusVO;
import com.institute.achievement.framework.dashboard.dto.DashboardTrendVO;
import com.institute.achievement.framework.dashboard.dto.DashboardTypeDistVO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * Service interface for dashboard statistics aggregation and export.
 * <p>
 * Provides 4 chart data queries and 2 export methods (Excel + PDF).
 * All chart queries are cached in Redis with 5-minute TTL (D-04).
 * Chart type parameter for export is validated against a whitelist.
 *
 * <h3>Security mitigations</h3>
 * <ul>
 *   <li>T-3-01-01: chartType validated via service-layer whitelist in export methods</li>
 *   <li>T-3-01-02: SQL-layer dept_id injection via @DataScope on DashboardMapper</li>
 * </ul>
 */
public interface DashboardService {

    /**
     * Get annual trend of achievement counts grouped by year and type.
     *
     * @param deptId   optional department filter (null = no filter)
     * @param yearFrom optional start year filter (null = no lower bound)
     * @param yearTo   optional end year filter (null = no upper bound)
     * @return list of trend data points
     */
    List<DashboardTrendVO> getAnnualTrend(Long deptId, Integer yearFrom, Integer yearTo);

    /**
     * Get type distribution of all achievements (paper / patent / software).
     *
     * @param deptId optional department filter (null = no filter)
     * @return list of type distribution data points with percentages computed
     */
    List<DashboardTypeDistVO> getTypeDist(Long deptId);

    /**
     * Get department ranking by total achievement count.
     *
     * @param deptId optional department filter (null = no filter)
     * @return list of department ranking data points
     */
    List<DashboardDeptRankVO> getDeptRanking(Long deptId);

    /**
     * Get patent legal status distribution (VALID / INVALID / UNKNOWN).
     *
     * @param deptId optional department filter (null = no filter)
     * @return list of patent status data points with percentages computed
     */
    List<DashboardPatentStatusVO> getPatentStatus(Long deptId);

    /**
     * Export chart detail data to Excel via EasyExcel streaming.
     * <p>
     * On query failure, writes a JSON error response to the response stream
     * instead of throwing an exception.
     *
     * @param response  the HTTP response to write the Excel stream to
     * @param chartType the chart type key (validated against whitelist)
     * @param deptId    optional department filter (null = no filter)
     */
    void exportExcel(HttpServletResponse response, String chartType, Long deptId);

    /**
     * Export chart report to PDF via iText 7.
     * <p>
     * Generates a formatted PDF report with table data from the chart.
     * Uses embedded Chinese font (NotoSansSC or STSong-Light fallback).
     *
     * @param response  the HTTP response to write the PDF stream to
     * @param chartType the chart type key (validated against whitelist)
     * @param deptId    optional department filter (null = no filter)
     */
    void exportPdf(HttpServletResponse response, String chartType, Long deptId);
}
