package com.institute.achievement.fee.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fee record entity — maps to the {@code fee_record} table.
 * <p>
 * Records fees associated with patents and software copyrights via the
 * Arc polymorphic pattern ({@code ownerType} / {@code ownerId}).
 * Supports the full fee lifecycle: pending → paid | paused.
 *
 * @see com.institute.achievement.fee.enums.FeeTypeEnum
 * @see com.institute.achievement.fee.enums.FeeStatusEnum
 */
@Data
@TableName("fee_record")
public class FeeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    // ── Fee Type ──────────────────────────────────────────────────────

    /** Fee type code — maps to {@link com.institute.achievement.fee.enums.FeeTypeEnum} */
    @TableField("fee_type")
    private String feeType;

    // ── Amounts ───────────────────────────────────────────────────────

    /** Total fee amount (yuan) */
    private BigDecimal amount;

    /** Actual paid amount (yuan) — may differ from amount if partial payment */
    @TableField("paid_amount")
    private BigDecimal paidAmount;

    // ── Dates ─────────────────────────────────────────────────────────

    /** Fee due date */
    @TableField("due_date")
    private LocalDate dueDate;

    /** Actual payment date */
    @TableField("paid_date")
    private LocalDate paidDate;

    // ── Payment Evidence ──────────────────────────────────────────────

    /** Payment voucher / receipt number */
    @TableField("voucher_no")
    private String voucherNo;

    // ── Status ────────────────────────────────────────────────────────

    /** Fee status code — maps to {@link com.institute.achievement.fee.enums.FeeStatusEnum} */
    private String status;

    // ── Financial ─────────────────────────────────────────────────────

    /** Funding source code — configured via data dictionary (FUND_SOURCE) */
    @TableField("funding_source")
    private String fundingSource;

    // ── Polymorphic Arc (D-10) ────────────────────────────────────────

    /** Owner type — "patent" or "copyright" */
    @TableField("owner_type")
    private String ownerType;

    /** Owner entity ID */
    @TableField("owner_id")
    private Long ownerId;

    // ── Source ────────────────────────────────────────────────────────

    /** Record source — "auto_generated" or "manual" */
    private String source;

    // ── Batch Slip (used in 02-04) ────────────────────────────────────

    /** Batch slip number, filled during batch slip generation */
    @TableField("slip_no")
    private String slipNo;

    /** Timestamp when the batch slip was generated */
    @TableField("slip_generated_time")
    private LocalDateTime slipGeneratedTime;

    /** User ID who generated the batch slip */
    @TableField("slip_generated_by")
    private Long slipGeneratedBy;

    // ── Department & Ownership ────────────────────────────────────────

    /** Department ID (for SQL-layer data isolation) */
    @TableField("dept_id")
    private Long deptId;

    /** Creator user ID */
    @TableField("created_by")
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /** Last updater user ID */
    @TableField("updated_by")
    private Long updatedBy;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedTime;
}
