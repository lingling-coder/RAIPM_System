package com.institute.achievement.framework.dashboard.service.impl;

import com.alibaba.excel.EasyExcel;
import com.institute.achievement.framework.dashboard.dto.DashboardDeptRankVO;
import com.institute.achievement.framework.dashboard.dto.DashboardExportVO;
import com.institute.achievement.framework.dashboard.dto.DashboardPatentStatusVO;
import com.institute.achievement.framework.dashboard.dto.DashboardSummaryVO;
import com.institute.achievement.framework.dashboard.dto.DashboardTrendVO;
import com.institute.achievement.framework.dashboard.dto.DashboardTypeDistVO;
import com.institute.achievement.framework.dashboard.mapper.DashboardMapper;
import com.institute.achievement.framework.dashboard.service.DashboardPdfService;
import com.institute.achievement.framework.dashboard.service.DashboardService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dashboard statistics service implementation.
 * <p>
 * Provides aggregation queries across 3 achievement tables with Redis 5-minute
 * caching (D-04) and EasyExcel/iText export capabilities (D-06, D-07).
 * Chart type parameter is validated against a whitelist before passing
 * to the mapper (T-3-01-01 mitigation).
 *
 * <h3>Security mitigations</h3>
 * <ul>
 *   <li>T-3-01-01: chartType validated via service-layer whitelist in export methods</li>
 *   <li>T-3-01-02: SQL-layer dept_id injection via @DataScope on DashboardMapper</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;
    private final DashboardPdfService dashboardPdfService;

    /**
     * Whitelist of valid chart type values (T-3-01-01 mitigation).
     * Prevents injection via the chartType parameter used in
     * MyBatis XML &lt;choose&gt; blocks.
     */
    private static final Set<String> VALID_CHART_TYPES = new HashSet<>(Arrays.asList(
            "annualTrend", "typeDist", "deptRanking", "patentStatus"
    ));

    @Cacheable(value = "dashboard", key = "#root.methodName + ':' + T(java.util.Objects).hash(#deptId) + ':' + T(java.util.Objects).hash(#yearFrom) + ':' + T(java.util.Objects).hash(#yearTo)", unless = "#result == null")
    @Override
    public List<DashboardTrendVO> getAnnualTrend(Long deptId, Integer yearFrom, Integer yearTo) {
        List<DashboardTrendVO> results = dashboardMapper.getAnnualTrend(deptId, yearFrom, yearTo);
        log.debug("Dashboard annual trend: {} rows", results.size());
        return results;
    }

    @Cacheable(value = "dashboard", key = "#root.methodName + ':' + T(java.util.Objects).hash(#deptId)", unless = "#result == null")
    @Override
    public List<DashboardTypeDistVO> getTypeDist(Long deptId) {
        List<DashboardTypeDistVO> results = dashboardMapper.getTypeDist(deptId);
        // Compute percentage for each type
        long total = results.stream().mapToLong(DashboardTypeDistVO::getCount).sum();
        if (total > 0) {
            for (DashboardTypeDistVO vo : results) {
                double pct = (vo.getCount() * 100.0) / total;
                vo.setPercentage(Math.round(pct * 10.0) / 10.0); // Round to 1 decimal
            }
        }
        log.debug("Dashboard type distribution: {} rows", results.size());
        return results;
    }

    @Cacheable(value = "dashboard", key = "#root.methodName + ':' + T(java.util.Objects).hash(#deptId)", unless = "#result == null")
    @Override
    public List<DashboardDeptRankVO> getDeptRanking(Long deptId) {
        List<DashboardDeptRankVO> results = dashboardMapper.getDeptRanking(deptId);
        log.debug("Dashboard dept ranking: {} rows", results.size());
        return results;
    }

    @Cacheable(value = "dashboard", key = "#root.methodName + ':' + T(java.util.Objects).hash(#deptId)", unless = "#result == null")
    @Override
    public List<DashboardPatentStatusVO> getPatentStatus(Long deptId) {
        List<DashboardPatentStatusVO> results = dashboardMapper.getPatentStatus(deptId);
        // Compute percentage for each status
        long total = results.stream().mapToLong(DashboardPatentStatusVO::getCount).sum();
        if (total > 0) {
            for (DashboardPatentStatusVO vo : results) {
                double pct = (vo.getCount() * 100.0) / total;
                vo.setPercentage(Math.round(pct * 10.0) / 10.0); // Round to 1 decimal
            }
        }
        log.debug("Dashboard patent status: {} rows", results.size());
        return results;
    }

    @Cacheable(value = "dashboard", key = "#root.methodName", unless = "#result == null")
    @Override
    public DashboardSummaryVO getSummary() {
        DashboardSummaryVO summary = dashboardMapper.getSummary();
        log.debug("Dashboard summary: {} users, {} depts, {} roles",
                summary.getTotalUsers(), summary.getTotalDepts(), summary.getTotalRoles());
        return summary;
    }

    @Override
    public void exportExcel(HttpServletResponse response, String chartType, Long deptId) {
        // T-3-01-01: Validate chartType against whitelist
        if (chartType == null || !VALID_CHART_TYPES.contains(chartType)) {
            try {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":400,\"message\":\"无效的图表类型: " +
                        escapeJson(chartType) + "\"}");
            } catch (IOException ioEx) {
                log.error("Failed to write error response for invalid chartType", ioEx);
            }
            return;
        }

        // Query export data
        List<DashboardExportVO> data;
        try {
            data = dashboardMapper.exportChartData(chartType, deptId);
        } catch (Exception e) {
            log.error("Failed to query export data for chartType={}", chartType, e);
            try {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":500,\"message\":\"导出数据查询失败\"}");
            } catch (IOException ioEx) {
                log.error("Failed to write error response for export", ioEx);
            }
            return;
        }

        // Set response headers for Excel download
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = "dashboard-" + chartType + "-" + dateStr + ".xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFilename);

        // Stream Excel via EasyExcel
        try {
            EasyExcel.write(response.getOutputStream(), DashboardExportVO.class)
                    .sheet(getChartSheetName(chartType))
                    .doWrite(data);
            log.info("Dashboard Excel export {} completed: {} rows", chartType, data.size());
        } catch (IOException e) {
            log.error("Failed to write Excel export stream for {}", chartType, e);
        }
    }

    @Override
    public void exportPdf(HttpServletResponse response, String chartType, Long deptId) {
        dashboardPdfService.exportPdf(response, chartType, deptId);
    }

    // ── Internal Helpers ──────────────────────────────────────────────

    /**
     * Get the sheet name for a given chart type.
     */
    private String getChartSheetName(String chartType) {
        switch (chartType) {
            case "annualTrend":
                return "年度趋势";
            case "typeDist":
                return "类型分布";
            case "deptRanking":
                return "部门排行";
            case "patentStatus":
                return "专利状态";
            default:
                return chartType + "明细";
        }
    }

    /**
     * Escape a string for safe inclusion in JSON error response.
     * Escapes backslash, double quote, and control characters.
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
