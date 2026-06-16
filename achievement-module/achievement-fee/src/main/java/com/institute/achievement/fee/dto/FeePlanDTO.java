package com.institute.achievement.fee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fee plan create/update DTO.
 * <p>
 * For manual creation: all fields are writable.
 * For update (T-02-02-01): only {@code amount} and {@code fundingSource} are
 * copied from the DTO — {@code dueDate} and {@code patentId} are system-locked (D-17).
 */
@Data
public class FeePlanDTO {

    @NotNull(message = "关联专利ID不能为空")
    private Long patentId;

    @NotBlank(message = "费用类型不能为空")
    private String feeType;

    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    /** Fee due date — required for manual one-time plans, ignored for auto-generated */
    private LocalDate dueDate;

    /** Funding source code from data dictionary (FUND_SOURCE) */
    private String fundingSource;

    /** Plan source — defaults to "manual" for API-created plans */
    private String source = "manual";
}
