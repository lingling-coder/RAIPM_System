package com.institute.achievement.fee.dto;

import com.institute.achievement.fee.enums.FeeStatusEnum;
import com.institute.achievement.fee.enums.FeeTypeEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Fee record view object — output for API responses.
 * <p>
 * Includes computed fields for frontend display (statusLabel, feeTypeLabel)
 * and transient fields populated by the alert engine (alertLevel).
 */
@Data
public class FeeRecordVO {

    private Long id;
    private String feeType;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private String voucherNo;
    private String status;
    private String fundingSource;
    private String ownerType;
    private Long ownerId;
    private String source;
    private String slipNo;
    private LocalDateTime slipGeneratedTime;
    private Long slipGeneratedBy;
    private Long deptId;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;

    // ── Computed / Transient ──────────────────────────────────────────

    /** Patent or copyright name (loaded via JOIN) */
    private String ownerName;

    /** Flexible display map: patentName, applicationNo, etc. */
    private Map<String, Object> ownerInfo;

    /** Alert level — populated by alert engine (02-03) */
    private String alertLevel;

    /**
     * Get Chinese status label.
     */
    public String getStatusLabel() {
        if (status == null) return null;
        FeeStatusEnum e = FeeStatusEnum.fromCode(status);
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
