package com.institute.achievement.fee.dto;

import com.institute.achievement.fee.enums.FeePlanStatusEnum;
import com.institute.achievement.fee.enums.FeeTypeEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fee plan view object — output for API responses.
 * <p>
 * Includes computed labels for frontend display (statusLabel, feeTypeLabel)
 * and transient patent fields populated by JOIN query (patentName, patentType, applicationNo).
 */
@Data
public class FeePlanVO {

    private Long id;
    private Long patentId;
    private String feeType;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String status;
    private String source;
    private String fundingSource;
    private Long deptId;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;

    // ── Computed / Transient ──────────────────────────────────────────

    /** Patent name (loaded via JOIN with patent table) */
    private String patentName;

    /** Patent type (loaded via JOIN with patent table) */
    private String patentType;

    /** Patent application number (loaded via JOIN with patent table) */
    private String applicationNo;

    /**
     * Get Chinese status label.
     */
    public String getStatusLabel() {
        if (status == null) return null;
        FeePlanStatusEnum e = FeePlanStatusEnum.fromCode(status);
        return e != null ? e.getLabel() : status;
    }

    /**
     * Get Chinese fee type label.
     */
    public String getFeeTypeLabel() {
        if (feeType == null) return null;
        FeeTypeEnum e = FeeTypeEnum.fromCode(feeType);
        return e != null ? e.getLabel() : feeType;
    }
}
