package com.institute.achievement.fee.service.impl;

import com.alibaba.excel.EasyExcel;
import com.institute.achievement.fee.dto.FeeStatsExcelVO;
import com.institute.achievement.fee.dto.FeeStatsVO;
import com.institute.achievement.fee.mapper.FeeStatsMapper;
import com.institute.achievement.fee.service.FeeStatsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fee statistics service implementation.
 * <p>
 * Provides overview aggregation, multi-dimensional grouping (D-28), and
 * EasyExcel export (D-29). Dimension parameter is validated against a
 * whitelist before passing to the mapper (T-02-05-02 mitigation).
 *
 * <h3>Security mitigations</h3>
 * <ul>
 *   <li>T-02-05-01: SQL-layer dept_id injection via MyBatis-Plus interceptor</li>
 *   <li>T-02-05-02: Dimension whitelist validation prevents SQL injection</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeStatsServiceImpl implements FeeStatsService {

    private final FeeStatsMapper feeStatsMapper;

    /**
     * Whitelist of valid dimension values (T-02-05-02 mitigation).
     * Prevents SQL injection via the dimension parameter that is used
     * in MyBatis XML <choose> blocks.
     */
    private static final Set<String> VALID_DIMENSIONS = new HashSet<>(Arrays.asList(
            "dept_id", "YEAR(due_date)", "patent_type", "funding_source"
    ));

    @Override
    public FeeStatsVO getOverview(Long deptId, Integer year, String patentType, String fundingSource) {
        FeeStatsVO overview = feeStatsMapper.getOverview(deptId, year, patentType, fundingSource);
        log.debug("Fee stats overview: totalAmount={}, totalPaid={}, totalPending={}, totalOverdue={}, recordCount={}",
                overview.getTotalAmount(), overview.getTotalPaid(),
                overview.getTotalPending(), overview.getTotalOverdue(),
                overview.getRecordCount());
        return overview;
    }

    @Override
    public List<FeeStatsVO> getDimensionStats(String dimension, Long deptId, Integer year,
                                              String patentType, String fundingSource) {
        // T-02-05-02: Validate dimension against whitelist before passing to mapper
        if (dimension == null || !VALID_DIMENSIONS.contains(dimension)) {
            throw new IllegalArgumentException("无效的统计维度: " + dimension
                    + "，允许的值: " + String.join(", ", VALID_DIMENSIONS));
        }

        List<FeeStatsVO> results = feeStatsMapper.summarizeByDimension(dimension, deptId, year, patentType, fundingSource);
        log.debug("Fee stats dimension '{}' returned {} rows", dimension, results.size());
        return results;
    }

    @Override
    public void exportExcel(HttpServletResponse response, Long deptId, Integer year,
                            String patentType, String fundingSource) {
        // Query dimension stats (default to dept_id for export)
        List<FeeStatsVO> stats;
        try {
            stats = feeStatsMapper.summarizeByDimension("dept_id", deptId, year, patentType, fundingSource);
        } catch (Exception e) {
            log.error("Failed to query fee stats for export", e);
            try {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":500,\"message\":\"导出数据查询失败: " + e.getMessage() + "\"}");
            } catch (IOException ioEx) {
                log.error("Failed to write error response for export", ioEx);
            }
            return;
        }

        // Map to Excel VOs
        List<FeeStatsExcelVO> excelRows = new ArrayList<>(stats.size());
        for (FeeStatsVO row : stats) {
            String paymentRate = calculatePaymentRate(row.getTotalPaid(), row.getTotalAmount());
            excelRows.add(new FeeStatsExcelVO(
                    row.getDimensionValue(),
                    row.getRecordCount(),
                    row.getTotalAmount(),
                    row.getTotalPaid(),
                    row.getTotalPending(),
                    row.getTotalOverdue(),
                    paymentRate
            ));
        }

        // Set response headers for Excel download
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = "fee-stats-dept_id-" + dateStr + ".xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFilename);

        // Stream Excel via EasyExcel
        try {
            EasyExcel.write(response.getOutputStream(), FeeStatsExcelVO.class)
                    .sheet("费用统计")
                    .doWrite(excelRows);
            log.info("Fee stats Excel export completed: {} rows", excelRows.size());
        } catch (IOException e) {
            log.error("Failed to write Excel export stream", e);
            // Response may already be committed; log and return
        }
    }

    // ── Internal Helpers ──────────────────────────────────────────────

    /**
     * Calculate payment rate as a percentage string.
     *
     * @param paid   total paid amount
     * @param total  total amount
     * @return formatted percentage string, "0.00%" if total is zero
     */
    private String calculatePaymentRate(BigDecimal paid, BigDecimal total) {
        if (total == null || BigDecimal.ZERO.compareTo(total) == 0) {
            return "0.00%";
        }
        if (paid == null) {
            return "0.00%";
        }
        return paid.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }
}
