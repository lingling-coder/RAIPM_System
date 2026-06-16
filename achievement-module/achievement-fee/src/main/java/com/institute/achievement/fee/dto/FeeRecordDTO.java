package com.institute.achievement.fee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fee record create/update DTO.
 * <p>
 * Only whitelisted fields are editable via update (T-02-01-02 mitigation).
 * {@code dueDate}, {@code ownerType}, and {@code ownerId} are system-locked
 * after creation per D-17.
 */
@Data
public class FeeRecordDTO {

    @NotBlank(message = "费用类型不能为空")
    private String feeType;

    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    /** Paid amount (only set during payment marking) */
    private BigDecimal paidAmount;

    @NotNull(message = "缴费截止日期不能为空")
    private LocalDate dueDate;

    /** Payment voucher / receipt number */
    private String voucherNo;

    /** Fee status code */
    private String status;

    /** Funding source code from data dictionary (FUND_SOURCE) */
    private String fundingSource;

    @NotBlank(message = "关联成果类型不能为空")
    private String ownerType;

    @NotNull(message = "关联成果ID不能为空")
    private Long ownerId;

    /** Record source — defaults to "manual" */
    private String source = "manual";
}
