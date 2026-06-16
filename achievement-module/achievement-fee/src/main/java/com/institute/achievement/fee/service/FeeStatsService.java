package com.institute.achievement.fee.service;

import com.institute.achievement.fee.dto.FeeStatsVO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * Service interface for fee statistics aggregation and export.
 * <p>
 * Provides overview summary, multi-dimensional grouped statistics (D-28),
 * and EasyExcel export (D-29). All statistics are computed via MySQL GROUP BY
 * aggregation (not in-memory) per the plan's truths.
 */
public interface FeeStatsService {

    /**
     * Get overview summary of fee records with optional cross-filters.
     *
     * @param deptId        filter by department ID (null = no filter)
     * @param year          filter by due date year (null = no filter)
     * @param patentType    filter by patent type code (null = no filter)
     * @param fundingSource filter by funding source code (null = no filter)
     * @return overview with totalAmount, totalPaid, totalPending, totalOverdue, recordCount
     */
    FeeStatsVO getOverview(Long deptId, Integer year, String patentType, String fundingSource);

    /**
     * Get fee statistics grouped by a dimension with optional cross-filters.
     * <p>
     * Valid {@code dimension} values: dept_id, YEAR(due_date), patent_type, funding_source (D-28).
     * Invalid values throw {@link IllegalArgumentException}.
     *
     * @param dimension     the grouping dimension (validated against whitelist)
     * @param deptId        cross-filter by department ID
     * @param year          cross-filter by due date year
     * @param patentType    cross-filter by patent type code
     * @param fundingSource cross-filter by funding source code
     * @return list of dimension rows with aggregate values
     * @throws IllegalArgumentException if dimension is not in the whitelist
     */
    List<FeeStatsVO> getDimensionStats(String dimension, Long deptId, Integer year, String patentType, String fundingSource);

    /**
     * Export fee statistics to Excel via EasyExcel streaming.
     * <p>
     * Uses the current dimension query results and writes them to the response
     * as an Excel file with Content-Disposition attachment headers (D-29).
     *
     * @param response      the HTTP response to write the Excel stream to
     * @param deptId        cross-filter by department ID
     * @param year          cross-filter by due date year
     * @param patentType    cross-filter by patent type code
     * @param fundingSource cross-filter by funding source code
     */
    void exportExcel(HttpServletResponse response, Long deptId, Integer year, String patentType, String fundingSource);
}
