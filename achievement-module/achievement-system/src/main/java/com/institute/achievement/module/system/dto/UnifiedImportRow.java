package com.institute.achievement.module.system.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Unified Excel import row for all three achievement types.
 * <p>
 * Follows the unified template design from RESEARCH.md lines 708-715:
 * A single Excel sheet containing columns for paper, patent, and copyright fields.
 * The "类型" column differentiates which achievement type each row represents.
 * <p>
 * Implements D-17 (unified template) and D-19 (Excel format only).
 */
@Data
public class UnifiedImportRow {

    // ── Common Fields ──────────────────────────────────────────────────

    @ExcelProperty("类型")
    private String type;

    @ExcelProperty("标题")
    private String title;

    @ExcelProperty("作者")
    private String authors;

    @ExcelProperty("涉密标记")
    private String isClassified;

    @ExcelProperty("密级")
    private String classifiedLevel;

    @ExcelProperty("所属课题")
    private String projectRef;

    // ── Paper-specific Fields ──────────────────────────────────────────

    @ExcelProperty("DOI")
    private String doi;

    @ExcelProperty("期刊")
    private String journal;

    @ExcelProperty("影响因子")
    private BigDecimal impactFactor;

    @ExcelProperty("卷号")
    private Integer volume;

    @ExcelProperty("期号")
    private Integer issue;

    @ExcelProperty("页码")
    private String pages;

    @ExcelProperty("发表年份")
    private Integer publishYear;

    @ExcelProperty("收录情况")
    private String indexStatus;

    @ExcelProperty("中科院分区")
    private String zone;

    @ExcelProperty("摘要")
    private String abstractText;

    // ── Patent-specific Fields ─────────────────────────────────────────

    @ExcelProperty("申请号")
    private String applicationNo;

    @ExcelProperty("授权号")
    private String authorizationNo;

    @ExcelProperty("专利类型")
    private String patentType;

    @ExcelProperty("国别")
    private String country;

    @ExcelProperty("申请日")
    private String applicationDate;

    @ExcelProperty("授权日")
    private String authorizationDate;

    @ExcelProperty("年费下次缴费日")
    private String nextFeeDate;

    @ExcelProperty("法律状态")
    private String legalStatus;

    // ── Copyright-specific Fields ──────────────────────────────────────

    @ExcelProperty("登记号")
    private String registrationNo;

    @ExcelProperty("登记日期")
    private String registrationDate;

    @ExcelProperty("版本号")
    private String versionNo;

    @ExcelProperty("软件类别")
    private String softwareCategory;

    @ExcelProperty("著作权人")
    private String copyrightHolder;
}
