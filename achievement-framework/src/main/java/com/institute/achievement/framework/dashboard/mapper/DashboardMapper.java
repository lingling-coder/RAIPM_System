package com.institute.achievement.framework.dashboard.mapper;

import com.institute.achievement.framework.dashboard.dto.DashboardDeptRankVO;
import com.institute.achievement.framework.dashboard.dto.DashboardExportVO;
import com.institute.achievement.framework.dashboard.dto.DashboardPatentStatusVO;
import com.institute.achievement.framework.dashboard.dto.DashboardTrendVO;
import com.institute.achievement.framework.dashboard.dto.DashboardTypeDistVO;
import com.institute.achievement.framework.permission.DataScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis mapper for dashboard statistics aggregation queries.
 * <p>
 * All queries use MySQL UNION ALL across paper, patent, and software_copyright
 * tables with GROUP BY aggregation. Custom SQL is defined in
 * {@code mapper/DashboardMapper.xml}.
 *
 * <h3>Security</h3>
 * <ul>
 *   <li>T-3-01-01: chartType validated via service-layer whitelist in export methods</li>
 *   <li>T-3-01-02: @DataScope triggers DataPermissionInterceptor for dept_id injection</li>
 * </ul>
 */
@Mapper
public interface DashboardMapper {

    /**
     * Get annual trend of achievement counts grouped by year and type.
     *
     * @param deptId  optional department filter (null = no filter)
     * @param yearFrom optional start year filter (null = no lower bound)
     * @param yearTo   optional end year filter (null = no upper bound)
     * @return list of trend data points
     */
    @DataScope(deptAlias = "t")
    List<DashboardTrendVO> getAnnualTrend(
            @Param("deptId") Long deptId,
            @Param("yearFrom") Integer yearFrom,
            @Param("yearTo") Integer yearTo);

    /**
     * Get type distribution of all achievements (paper / patent / software).
     *
     * @param deptId optional department filter (null = no filter)
     * @return list of type distribution data points
     */
    @DataScope(deptAlias = "t")
    List<DashboardTypeDistVO> getTypeDist(
            @Param("deptId") Long deptId);

    /**
     * Get department ranking by total achievement count.
     *
     * @param deptId optional department filter (null = no filter)
     * @return list of department ranking data points
     */
    @DataScope(deptAlias = "t")
    List<DashboardDeptRankVO> getDeptRanking(
            @Param("deptId") Long deptId);

    /**
     * Get patent legal status distribution (VALID / INVALID / UNKNOWN).
     *
     * @param deptId optional department filter (null = no filter)
     * @return list of patent status data points
     */
    @DataScope(deptAlias = "t")
    List<DashboardPatentStatusVO> getPatentStatus(
            @Param("deptId") Long deptId);

    /**
     * Export dashboard chart data as raw rows.
     * <p>
     * Uses a {@code <choose>} block to select the appropriate detail query
     * per chart type (dimension whitelist pattern from FeeStats).
     *
     * @param chartType the chart type key: annualTrend, typeDist, deptRanking, patentStatus
     * @param deptId    optional department filter (null = no filter)
     * @return list of export data rows
     */
    List<DashboardExportVO> exportChartData(
            @Param("chartType") String chartType,
            @Param("deptId") Long deptId);
}
