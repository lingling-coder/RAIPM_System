package com.institute.achievement.patent.dto;

import com.institute.achievement.common.enums.AchievementStatusEnum;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Patent view object — output for API responses.
 * <p>
 * Includes computed fields like statusLabel (Chinese) for frontend display.
 */
@Data
public class PatentVO {

    private Long id;
    private String patentName;
    private String inventors;
    private String applicationNo;
    private String authorizationNo;
    private LocalDate applicationDate;
    private LocalDate authorizationDate;
    private String patentType;
    private String country;
    private LocalDate nextFeeDate;
    private String legalStatus;

    // ── Classification ──────────────────────────────────────────────

    private Integer isClassified;
    private String classifiedLevel;

    // ── Project Linkage ─────────────────────────────────────────────

    private String projectRef;

    // ── Lifecycle ───────────────────────────────────────────────────

    private String status;

    /** Chinese label for the current status (computed from AchievementStatusEnum) */
    private String statusLabel;

    private String archiveNo;

    // ── Department & Audit ──────────────────────────────────────────

    private Long deptId;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;

    // ── Computed ────────────────────────────────────────────────────

    /**
     * Get the Chinese status label.
     */
    public String getStatusLabel() {
        if (status == null) return null;
        try {
            return AchievementStatusEnum.fromName(status).getLabel();
        } catch (IllegalArgumentException e) {
            return status;
        }
    }
}
