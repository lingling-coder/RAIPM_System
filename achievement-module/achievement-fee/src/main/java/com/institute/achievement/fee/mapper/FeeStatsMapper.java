package com.institute.achievement.fee.mapper;

import com.institute.achievement.fee.dto.FeeStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis mapper for fee statistics aggregation queries.
 * <p>
 * All queries operate on the {@code fee_record} table with MySQL GROUP BY
 * aggregation and CASE WHEN for computed columns. Custom SQL is defined in
 * {@code mapper/FeeStatsMapper.xml}.
 *
 * <h3>Security</h3>
 * <ul>
 *   <li>T-02-05-01: SQL-layer dept_id injection via MyBatis-Plus interceptor</li>
 *   <li>T-02-05-02: Dimension parameter validated against whitelist in service layer</li>
 * </ul>
 */
@Mapper
public interface FeeStatsMapper {

    /**
     * Get overview summary of fee records with optional cross-filters.
     *
     * @param deptId       filter by department ID (null = no filter)
     * @param year         filter by due date year (null = no filter)
     * @param patentType   filter by patent.patent_type (null = no filter)
     * @param fundingSource filter by funding_source (null = no filter)
     * @return overview with totalAmount, totalPaid, totalPending, totalOverdue, recordCount
     */
    FeeStatsVO getOverview(
            @Param("deptId") Long deptId,
            @Param("year") Integer year,
            @Param("patentType") String patentType,
            @Param("fundingSource") String fundingSource);

    /**
     * Summarize fee records grouped by a dimension.
     * <p>
     * The {@code dimension} parameter must be one of:
     * {@code dept_id}, {@code YEAR(due_date)}, {@code patent_type}, {@code funding_source}.
     * Validated against whitelist in service layer (T-02-05-02).
     *
     * @param dimension    the grouping dimension
     * @param deptId       cross-filter by department ID
     * @param year         cross-filter by due date year
     * @param patentType   cross-filter by patent type
     * @param fundingSource cross-filter by funding source
     * @return list of dimension rows with aggregate values
     */
    List<FeeStatsVO> summarizeByDimension(
            @Param("dimension") String dimension,
            @Param("deptId") Long deptId,
            @Param("year") Integer year,
            @Param("patentType") String patentType,
            @Param("fundingSource") String fundingSource);
}
