package com.institute.achievement.framework.dashboard.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.institute.achievement.framework.dashboard.dto.DashboardDeptRankVO;
import com.institute.achievement.framework.dashboard.dto.DashboardExportVO;
import com.institute.achievement.framework.dashboard.dto.DashboardPatentStatusVO;
import com.institute.achievement.framework.dashboard.dto.DashboardTrendVO;
import com.institute.achievement.framework.dashboard.dto.DashboardTypeDistVO;
import com.institute.achievement.framework.dashboard.mapper.DashboardMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
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
 * PDF export service for dashboard chart data using iText 7.
 * <p>
 * Generates formatted PDF reports with table data from dashboard charts.
 * Uses embedded NotoSansSC font for Chinese character support, with
 * STSong-Light fallback via font-asian library if the .ttf is not found.
 *
 * <h3>Security mitigations</h3>
 * <ul>
 *   <li>T-3-01-01: chartType validated via whitelist</li>
 *   <li>T-3-01-03: iText 7 text/layout objects isolate data from structure</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardPdfService {

    private final DashboardMapper dashboardMapper;

    /**
     * Whitelist of valid chart type values (T-3-01-01 mitigation).
     */
    private static final Set<String> VALID_CHART_TYPES = new HashSet<>(Arrays.asList(
            "annualTrend", "typeDist", "deptRanking", "patentStatus"
    ));

    /**
     * Export dashboard chart data to PDF.
     * <p>
     * Generates a formatted report with:
     * - Title with chart type label and generation timestamp
     * - Data table with appropriate column headers per chart type
     * - Embedded Chinese font (or STSong-Light fallback)
     *
     * @param response  the HTTP response to write the PDF stream to
     * @param chartType the chart type key (validated against whitelist)
     * @param deptId    optional department filter (null = no filter)
     */
    public void exportPdf(HttpServletResponse response, String chartType, Long deptId) {
        // T-3-01-01: Validate chartType against whitelist
        if (chartType == null || !VALID_CHART_TYPES.contains(chartType)) {
            try {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":400,\"message\":\"无效的图表类型\"}");
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
            log.error("Failed to query PDF export data for chartType={}", chartType, e);
            try {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":500,\"message\":\"导出数据查询失败\"}");
            } catch (IOException ioEx) {
                log.error("Failed to write error response for PDF export", ioEx);
            }
            return;
        }

        // Set response headers for PDF download
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = "dashboard-" + chartType + "-" + dateStr + ".pdf";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        response.setContentType("application/pdf");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFilename);

        // Load Chinese font
        PdfFont chineseFont = loadChineseFont();

        // Generate PDF
        try (PdfWriter writer = new PdfWriter(response.getOutputStream());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // Add title
            String titleLabel = getChartLabel(chartType);
            document.add(new Paragraph(titleLabel + " - 统计报表")
                    .setFont(chineseFont).setFontSize(18));

            // Add timestamp
            document.add(new Paragraph("生成时间: " + LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .setFont(chineseFont).setFontSize(10));

            // Add data table
            Table table = createDataTable(chartType, data, chineseFont);
            if (table != null) {
                document.add(table);
            }

            log.info("Dashboard PDF export {} completed: {} rows", chartType, data.size());
        } catch (Exception e) {
            log.error("Failed to generate PDF report for {}", chartType, e);
            // Response may already be committed; log and return
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────

    /**
     * Load Chinese font for PDF rendering.
     * <p>
     * Tries to load embedded NotoSansSC-Regular.ttf from classpath first.
     * Falls back to STSong-Light via font-asian library if the .ttf is not found.
     *
     * @return the loaded PdfFont
     */
    private PdfFont loadChineseFont() {
        try {
            ClassPathResource fontResource = new ClassPathResource("fonts/NotoSansSC-Regular.ttf");
            if (fontResource.exists()) {
                PdfFont font = PdfFontFactory.createFont(
                        fontResource.getURL().toString(),
                        PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                log.debug("Loaded embedded NotoSansSC font for PDF");
                return font;
            }
        } catch (IOException e) {
            log.warn("Embedded NotoSansSC font not found, falling back to STSong-Light", e);
        }

        // Fallback to STSong-Light via font-asian
        try {
            PdfFont font = PdfFontFactory.createFont(
                    "STSong-Light",
                    "UniGB-UCS2-H",
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            log.debug("Using STSong-Light fallback font for PDF");
            return font;
        } catch (IOException e) {
            log.error("Failed to load STSong-Light fallback font", e);
            // Ultimate fallback: use the default font (may not render Chinese correctly)
            return null;
        }
    }

    /**
     * Create a data table for the specified chart type.
     *
     * @param chartType    the chart type key
     * @param data         the export data rows
     * @param chineseFont  the loaded Chinese font
     * @return a formatted Table, or null if data is empty
     */
    private Table createDataTable(String chartType, List<DashboardExportVO> data, PdfFont chineseFont) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        Table table;
        switch (chartType) {
            case "annualTrend":
                table = new Table(UnitValue.createPercentArray(new float[]{2, 2, 2}));
                table.addHeaderCell(new Paragraph("年度").setFont(chineseFont));
                table.addHeaderCell(new Paragraph("成果类型").setFont(chineseFont));
                table.addHeaderCell(new Paragraph("数量").setFont(chineseFont));
                for (DashboardExportVO row : data) {
                    table.addCell(new Paragraph(nullToEmpty(row.getYear())).setFont(chineseFont));
                    table.addCell(new Paragraph(getTypeLabel(row.getAchievementType())).setFont(chineseFont));
                    table.addCell(new Paragraph(String.valueOf(row.getCount())).setFont(chineseFont));
                }
                break;

            case "typeDist":
                table = new Table(UnitValue.createPercentArray(new float[]{2, 2, 2}));
                table.addHeaderCell(new Paragraph("成果类型").setFont(chineseFont));
                table.addHeaderCell(new Paragraph("数量").setFont(chineseFont));
                table.addHeaderCell(new Paragraph("占比").setFont(chineseFont));
                for (DashboardExportVO row : data) {
                    table.addCell(new Paragraph(getTypeLabel(row.getAchievementType())).setFont(chineseFont));
                    table.addCell(new Paragraph(String.valueOf(row.getCount())).setFont(chineseFont));
                    table.addCell(new Paragraph(nullToEmpty(row.getPercentage())).setFont(chineseFont));
                }
                break;

            case "deptRanking":
                table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2, 2}));
                table.addHeaderCell(new Paragraph("部门名称").setFont(chineseFont));
                table.addHeaderCell(new Paragraph("论文").setFont(chineseFont));
                table.addHeaderCell(new Paragraph("专利").setFont(chineseFont));
                table.addHeaderCell(new Paragraph("软著").setFont(chineseFont));
                table.addHeaderCell(new Paragraph("合计").setFont(chineseFont));
                for (DashboardExportVO row : data) {
                    table.addCell(new Paragraph(nullToEmpty(row.getDeptName())).setFont(chineseFont));
                    table.addCell(new Paragraph(String.valueOf(row.getCount())).setFont(chineseFont));
                    table.addCell(new Paragraph("").setFont(chineseFont));
                    table.addCell(new Paragraph("").setFont(chineseFont));
                    table.addCell(new Paragraph(String.valueOf(row.getCount())).setFont(chineseFont));
                }
                break;

            case "patentStatus":
                table = new Table(UnitValue.createPercentArray(new float[]{2, 2, 2}));
                table.addHeaderCell(new Paragraph("状态").setFont(chineseFont));
                table.addHeaderCell(new Paragraph("数量").setFont(chineseFont));
                table.addHeaderCell(new Paragraph("占比").setFont(chineseFont));
                for (DashboardExportVO row : data) {
                    table.addCell(new Paragraph(getPatentStatusLabel(row.getStatus())).setFont(chineseFont));
                    table.addCell(new Paragraph(String.valueOf(row.getCount())).setFont(chineseFont));
                    table.addCell(new Paragraph(nullToEmpty(row.getPercentage())).setFont(chineseFont));
                }
                break;

            default:
                table = new Table(UnitValue.createPercentArray(new float[]{2, 2, 2, 2}));
                table.addHeaderCell(new Paragraph("年度").setFont(chineseFont));
                table.addHeaderCell(new Paragraph("成果类型").setFont(chineseFont));
                table.addHeaderCell(new Paragraph("数量").setFont(chineseFont));
                table.addHeaderCell(new Paragraph("部门").setFont(chineseFont));
                for (DashboardExportVO row : data) {
                    table.addCell(new Paragraph(nullToEmpty(row.getYear())).setFont(chineseFont));
                    table.addCell(new Paragraph(getTypeLabel(row.getAchievementType())).setFont(chineseFont));
                    table.addCell(new Paragraph(String.valueOf(row.getCount())).setFont(chineseFont));
                    table.addCell(new Paragraph(nullToEmpty(row.getDeptName())).setFont(chineseFont));
                }
                break;
        }

        return table;
    }

    /**
     * Get the Chinese label for a chart type.
     */
    private String getChartLabel(String chartType) {
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
                return chartType;
        }
    }

    /**
     * Get the Chinese label for an achievement type.
     */
    private String getTypeLabel(String type) {
        if (type == null) return "";
        switch (type) {
            case "paper":
                return "论文";
            case "patent":
                return "专利";
            case "software":
                return "软著";
            default:
                return type;
        }
    }

    /**
     * Get the Chinese label for a patent status.
     */
    private String getPatentStatusLabel(String status) {
        if (status == null) return "";
        switch (status) {
            case "VALID":
                return "有效";
            case "INVALID":
                return "无效";
            case "UNKNOWN":
                return "未知";
            default:
                return status;
        }
    }

    /**
     * Convert null to empty string for safe PDF rendering.
     */
    private String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
    }
}
