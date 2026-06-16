package com.institute.achievement.fee.dto;

import com.institute.achievement.fee.enums.AlertLevelEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Alert record view object — output for API responses.
 * <p>
 * Includes JOIN fields from fee_record (feeAmount, dueDate) and
 * owner name resolved from patent/copyright tables.
 */
@Data
public class AlertRecordVO {

    private Long id;
    private Long feeRecordId;
    private String alertLevel;
    private LocalDate triggeredDate;
    private LocalDateTime triggeredAt;
    private LocalDateTime resolvedAt;
    private String status;
    private String escalationLevel;
    private BigDecimal feeAmount;
    private LocalDate dueDate;
    private String ownerType;
    private Long ownerId;
    private String ownerName;

    // ── Computed / Transient ──────────────────────────────────────────

    /**
     * Get Chinese alert level label from enum.
     */
    public String getAlertLevelLabel() {
        if (alertLevel == null) return null;
        AlertLevelEnum e = AlertLevelEnum.fromCode(alertLevel);
        return e != null ? e.getLabel() : alertLevel;
    }

    /**
     * Get Chinese status label.
     */
    public String getStatusLabel() {
        if (status == null) return null;
        switch (status) {
            case "pending": return "待处理";
            case "resolved": return "已处理";
            case "ignored": return "已忽略";
            default: return status;
        }
    }

    /**
     * Get Chinese escalation level label.
     */
    public String getEscalationLevelLabel() {
        if (escalationLevel == null) return null;
        switch (escalationLevel) {
            case "NONE": return "无";
            case "FIRST_ALERT": return "首次预警";
            case "DEPT_HEAD": return "部门负责人";
            case "LEADERSHIP": return "院领导";
            default: return escalationLevel;
        }
    }
}
