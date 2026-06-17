package com.institute.achievement.framework.dashboard.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * EasyExcel model for dashboard chart data export.
 * <p>
 * Contains common fields for all chart types:
 * <ul>
 *   <li>year — the year context (as string for display)</li>
 *   <li>achievementType — 'paper', 'patent', or 'software'</li>
 *   <li>count — the aggregate count</li>
 * </ul>
 * Chart-specific fields (deptName, status, percentage) are included
 * for richer export content where applicable.
 */
@Data
@AllArgsConstructor
public class DashboardExportVO {

    @ExcelProperty("年度")
    private String year;

    @ExcelProperty("成果类型")
    private String achievementType;

    @ExcelProperty("数量")
    private Long count;

    @ExcelProperty("部门名称")
    private String deptName;

    @ExcelProperty("专利状态")
    private String status;

    @ExcelProperty("占比")
    private String percentage;
}
