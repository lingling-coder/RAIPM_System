package com.institute.achievement.framework.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * VO for department ranking bar chart data.
 * <p>
 * Represents the count of achievements by department, broken down
 * by achievement type. Used by the department ranking bar chart
 * on the dashboard. Ordered by totalCount descending.
 */
@Data
@AllArgsConstructor
public class DashboardDeptRankVO {

    /** Department name */
    private String deptName;

    /** Department ID */
    private Long deptId;

    /** Count of paper achievements */
    private Long paperCount;

    /** Count of patent achievements */
    private Long patentCount;

    /** Count of software copyright achievements */
    private Long softwareCount;

    /** Total count across all achievement types */
    private Long totalCount;
}
