package com.institute.achievement.framework.dashboard;

import com.institute.achievement.framework.dashboard.dto.DashboardDeptRankVO;
import com.institute.achievement.framework.dashboard.dto.DashboardExportVO;
import com.institute.achievement.framework.dashboard.dto.DashboardPatentStatusVO;
import com.institute.achievement.framework.dashboard.dto.DashboardTrendVO;
import com.institute.achievement.framework.dashboard.dto.DashboardTypeDistVO;
import com.institute.achievement.framework.dashboard.mapper.DashboardMapper;
import com.institute.achievement.framework.dashboard.service.DashboardPdfService;
import com.institute.achievement.framework.dashboard.service.impl.DashboardServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.ServletOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DashboardServiceImpl.
 * <p>
 * Tests all 4 chart query methods, Excel export (success + error paths),
 * and verifies that the service correctly delegates to the mapper and
 * handles edge cases (null deptId, empty results, query failures).
 *
 * @see DashboardServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardMapper dashboardMapper;

    @Mock
    private DashboardPdfService dashboardPdfService;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private List<DashboardTrendVO> sampleTrendData;
    private List<DashboardTypeDistVO> sampleTypeDistData;
    private List<DashboardDeptRankVO> sampleDeptRankData;
    private List<DashboardPatentStatusVO> samplePatentStatusData;

    @BeforeEach
    void setUp() {
        // Sample annual trend data
        sampleTrendData = Arrays.asList(
                createTrend(2022, "paper", 5L),
                createTrend(2022, "patent", 3L),
                createTrend(2022, "software", 2L),
                createTrend(2023, "paper", 8L),
                createTrend(2023, "patent", 4L),
                createTrend(2023, "software", 3L)
        );

        // Sample type distribution data
        sampleTypeDistData = Arrays.asList(
                createTypeDist("paper", 13L),
                createTypeDist("patent", 7L),
                createTypeDist("software", 5L)
        );

        // Sample department ranking data
        sampleDeptRankData = Arrays.asList(
                createDeptRank("计算机学院", 1L, 10L, 5L, 3L, 18L),
                createDeptRank("电子学院", 2L, 8L, 4L, 2L, 14L),
                createDeptRank("数学学院", 3L, 5L, 2L, 1L, 8L)
        );

        // Sample patent status data
        samplePatentStatusData = Arrays.asList(
                createPatentStatus("VALID", "有效", 15L),
                createPatentStatus("INVALID", "无效", 5L),
                createPatentStatus("UNKNOWN", "未知", 2L)
        );
    }

    // ── getAnnualTrend Tests ─────────────────────────────────────────

    @Test
    @DisplayName("getAnnualTrend should return data when mapper returns results")
    void getAnnualTrend_WithData_ReturnsTrendList() {
        when(dashboardMapper.getAnnualTrend(null, null, null)).thenReturn(sampleTrendData);

        List<DashboardTrendVO> result = dashboardService.getAnnualTrend(null, null, null);

        assertNotNull(result);
        assertEquals(6, result.size());
        assertEquals(2022, result.get(0).getYear());
        assertEquals("paper", result.get(0).getAchievementType());
        assertEquals(5L, result.get(0).getCount());
        verify(dashboardMapper).getAnnualTrend(null, null, null);
    }

    @Test
    @DisplayName("getAnnualTrend should return empty list when mapper returns no results")
    void getAnnualTrend_NoData_ReturnsEmptyList() {
        when(dashboardMapper.getAnnualTrend(null, null, null)).thenReturn(Collections.emptyList());

        List<DashboardTrendVO> result = dashboardService.getAnnualTrend(null, null, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(dashboardMapper).getAnnualTrend(null, null, null);
    }

    @Test
    @DisplayName("getAnnualTrend should pass deptId filter to mapper")
    void getAnnualTrend_WithDeptId_PassesFilterToMapper() {
        when(dashboardMapper.getAnnualTrend(100L, 2022, 2023)).thenReturn(sampleTrendData);

        List<DashboardTrendVO> result = dashboardService.getAnnualTrend(100L, 2022, 2023);

        assertNotNull(result);
        assertEquals(6, result.size());
        verify(dashboardMapper).getAnnualTrend(100L, 2022, 2023);
    }

    // ── getTypeDist Tests ───────────────────────────────────────────

    @Test
    @DisplayName("getTypeDist should return data with computed percentages")
    void getTypeDist_WithData_ReturnsDistWithPercentages() {
        when(dashboardMapper.getTypeDist(null)).thenReturn(sampleTypeDistData);

        List<DashboardTypeDistVO> result = dashboardService.getTypeDist(null);

        assertNotNull(result);
        assertEquals(3, result.size());
        // Total = 13 + 7 + 5 = 25
        // paper = 13/25 * 100 = 52.0
        assertEquals(52.0, result.get(0).getPercentage(), 0.1);
        // patent = 7/25 * 100 = 28.0
        assertEquals(28.0, result.get(1).getPercentage(), 0.1);
        // software = 5/25 * 100 = 20.0
        assertEquals(20.0, result.get(2).getPercentage(), 0.1);
        verify(dashboardMapper).getTypeDist(null);
    }

    @Test
    @DisplayName("getTypeDist should return empty list when mapper returns no results")
    void getTypeDist_NoData_ReturnsEmptyList() {
        when(dashboardMapper.getTypeDist(null)).thenReturn(Collections.emptyList());

        List<DashboardTypeDistVO> result = dashboardService.getTypeDist(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(dashboardMapper).getTypeDist(null);
    }

    @Test
    @DisplayName("getTypeDist should pass deptId filter to mapper")
    void getTypeDist_WithDeptId_PassesFilterToMapper() {
        when(dashboardMapper.getTypeDist(100L)).thenReturn(sampleTypeDistData);

        List<DashboardTypeDistVO> result = dashboardService.getTypeDist(100L);

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(dashboardMapper).getTypeDist(100L);
    }

    // ── getDeptRanking Tests ────────────────────────────────────────

    @Test
    @DisplayName("getDeptRanking should return data when mapper returns results")
    void getDeptRanking_WithData_ReturnsRankingList() {
        when(dashboardMapper.getDeptRanking(null)).thenReturn(sampleDeptRankData);

        List<DashboardDeptRankVO> result = dashboardService.getDeptRanking(null);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("计算机学院", result.get(0).getDeptName());
        assertEquals(18L, result.get(0).getTotalCount());
        verify(dashboardMapper).getDeptRanking(null);
    }

    @Test
    @DisplayName("getDeptRanking should return empty list when mapper returns no results")
    void getDeptRanking_NoData_ReturnsEmptyList() {
        when(dashboardMapper.getDeptRanking(null)).thenReturn(Collections.emptyList());

        List<DashboardDeptRankVO> result = dashboardService.getDeptRanking(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(dashboardMapper).getDeptRanking(null);
    }

    @Test
    @DisplayName("getDeptRanking should pass deptId filter to mapper")
    void getDeptRanking_WithDeptId_PassesFilterToMapper() {
        when(dashboardMapper.getDeptRanking(100L)).thenReturn(sampleDeptRankData);

        List<DashboardDeptRankVO> result = dashboardService.getDeptRanking(100L);

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(dashboardMapper).getDeptRanking(100L);
    }

    // ── getPatentStatus Tests ───────────────────────────────────────

    @Test
    @DisplayName("getPatentStatus should return data with computed percentages")
    void getPatentStatus_WithData_ReturnsStatusWithPercentages() {
        when(dashboardMapper.getPatentStatus(null)).thenReturn(samplePatentStatusData);

        List<DashboardPatentStatusVO> result = dashboardService.getPatentStatus(null);

        assertNotNull(result);
        assertEquals(3, result.size());
        // Total = 15 + 5 + 2 = 22
        // VALID = 15/22 * 100 = 68.2
        assertEquals(68.2, result.get(0).getPercentage(), 0.1);
        // INVALID = 5/22 * 100 = 22.7
        assertEquals(22.7, result.get(1).getPercentage(), 0.1);
        // UNKNOWN = 2/22 * 100 = 9.1
        assertEquals(9.1, result.get(2).getPercentage(), 0.1);
        verify(dashboardMapper).getPatentStatus(null);
    }

    @Test
    @DisplayName("getPatentStatus should return empty list when mapper returns no results")
    void getPatentStatus_NoData_ReturnsEmptyList() {
        when(dashboardMapper.getPatentStatus(null)).thenReturn(Collections.emptyList());

        List<DashboardPatentStatusVO> result = dashboardService.getPatentStatus(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(dashboardMapper).getPatentStatus(null);
    }

    @Test
    @DisplayName("getPatentStatus should pass deptId filter to mapper")
    void getPatentStatus_WithDeptId_PassesFilterToMapper() {
        when(dashboardMapper.getPatentStatus(100L)).thenReturn(samplePatentStatusData);

        List<DashboardPatentStatusVO> result = dashboardService.getPatentStatus(100L);

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(dashboardMapper).getPatentStatus(100L);
    }

    // ── exportExcel Tests ───────────────────────────────────────────

    @Test
    @DisplayName("exportExcel should invoke mapper when chartType is valid")
    void exportExcel_ValidChartType_QueriesMapper(@Mock HttpServletResponse response) throws Exception {
        List<DashboardExportVO> exportData = Arrays.asList(
                new DashboardExportVO("2022", "paper", 5L, "", "", ""),
                new DashboardExportVO("2023", "paper", 8L, "", "", "")
        );
        when(dashboardMapper.exportChartData("annualTrend", null)).thenReturn(exportData);
        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outputStream);

        dashboardService.exportExcel(response, "annualTrend", null);

        verify(dashboardMapper).exportChartData("annualTrend", null);
        verify(response).setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        verify(response).setHeader(eq("Content-disposition"), startsWith("attachment;filename*=utf-8''"));
    }

    @Test
    @DisplayName("exportExcel should write JSON error when chartType is invalid")
    void exportExcel_InvalidChartType_WritesJsonError(@Mock HttpServletResponse response) throws Exception {
        PrintWriter mockWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(mockWriter);

        dashboardService.exportExcel(response, "invalidType", null);

        verify(dashboardMapper, never()).exportChartData(anyString(), any());
        verify(mockWriter).write(contains("无效的图表类型"));
    }

    @Test
    @DisplayName("exportExcel should write JSON error when query fails")
    void exportExcel_QueryError_WritesJsonError(@Mock HttpServletResponse response) throws Exception {
        when(dashboardMapper.exportChartData("annualTrend", null)).thenThrow(new RuntimeException("DB error"));
        PrintWriter mockWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(mockWriter);

        dashboardService.exportExcel(response, "annualTrend", null);

        verify(mockWriter).write(contains("导出数据查询失败"));
    }

    @Test
    @DisplayName("exportExcel should work with deptId filter")
    void exportExcel_WithDeptId_PassesFilterToMapper(@Mock HttpServletResponse response) throws Exception {
        when(dashboardMapper.exportChartData("annualTrend", 100L)).thenReturn(Collections.emptyList());
        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outputStream);

        dashboardService.exportExcel(response, "annualTrend", 100L);

        verify(dashboardMapper).exportChartData("annualTrend", 100L);
    }

    // ── exportPdf Tests ─────────────────────────────────────────────

    @Test
    @DisplayName("exportPdf should delegate to DashboardPdfService")
    void exportPdf_ValidChartType_DelegatesToPdfService(@Mock HttpServletResponse response) {
        dashboardService.exportPdf(response, "annualTrend", null);

        verify(dashboardPdfService).exportPdf(response, "annualTrend", null);
    }

    // ── Test Helpers ────────────────────────────────────────────────

    private DashboardTrendVO createTrend(Integer year, String type, Long count) {
        return new DashboardTrendVO(year, type, count);
    }

    private DashboardTypeDistVO createTypeDist(String type, Long count) {
        return new DashboardTypeDistVO(type, count, 0.0);
    }

    private DashboardDeptRankVO createDeptRank(String deptName, Long deptId,
                                                 Long paperCount, Long patentCount,
                                                 Long softwareCount, Long totalCount) {
        return new DashboardDeptRankVO(deptName, deptId, paperCount, patentCount, softwareCount, totalCount);
    }

    private DashboardPatentStatusVO createPatentStatus(String status, String label, Long count) {
        return new DashboardPatentStatusVO(status, label, count, 0.0);
    }
}
