package com.institute.achievement.fee.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * EasyExcel model for fee statistics export (Plan 02-05, D-29).
 */
@Data
@AllArgsConstructor
public class FeeStatsExcelVO {

    @ExcelProperty("维度")
    private String dimension;

    @ExcelProperty("记录数")
    private Long recordCount;

    @ExcelProperty("总金额")
    private BigDecimal totalAmount;

    @ExcelProperty("已缴费")
    private BigDecimal totalPaid;

    @ExcelProperty("待缴费")
    private BigDecimal totalPending;

    @ExcelProperty("逾期金额")
    private BigDecimal totalOverdue;

    @ExcelProperty("缴费率")
    private String paymentRate;
}
