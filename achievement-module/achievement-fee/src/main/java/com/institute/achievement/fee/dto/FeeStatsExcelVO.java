package com.institute.achievement.fee.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * EasyExcel model for fee statistics export (Plan 02-05, D-29).
 * <p>
 * Annotated with {@link ExcelProperty} for column headers and ordering,
 * and {@link ColumnWidth} for readable column widths.
 */
@Data
@AllArgsConstructor
public class FeeStatsExcelVO {

    @ExcelProperty("维度")
    @ColumnWidth(20)
    private String dimension;

    @ExcelProperty("记录数")
    @ColumnWidth(15)
    private Long recordCount;

    @ExcelProperty("总金额")
    @ColumnWidth(18)
    private BigDecimal totalAmount;

    @ExcelProperty("已缴费")
    @ColumnWidth(18)
    private BigDecimal totalPaid;

    @ExcelProperty("待缴费")
    @ColumnWidth(18)
    private BigDecimal totalPending;

    @ExcelProperty("逾期金额")
    @ColumnWidth(18)
    private BigDecimal totalOverdue;

    @ExcelProperty("缴费率")
    @ColumnWidth(15)
    private String paymentRate;
}
