package com.institute.achievement.copyright.dto;

import com.institute.achievement.common.enums.AchievementStatusEnum;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Software copyright view object — output for API responses.
 * <p>
 * Includes computed fields like statusLabel (Chinese) for frontend display.
 */
@Data
public class CopyrightVO {

    private Long id;
    private String name;
    private String copyrightHolder;
    private String registrationNo;
    private LocalDate registrationDate;
    private String softwareVersion;
    private String softwareCategory;

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
    private String submitterName;
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
