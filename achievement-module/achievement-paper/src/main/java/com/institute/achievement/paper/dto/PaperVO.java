package com.institute.achievement.paper.dto;

import com.institute.achievement.common.enums.AchievementStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Paper view object — output for API responses.
 * <p>
 * Includes computed fields like statusLabel (Chinese) for frontend display.
 */
@Data
public class PaperVO {

    private Long id;
    private String title;
    private String authors;
    private String journal;
    private String doi;
    private String issn;
    private Integer volume;
    private Integer issue;
    private String pages;
    private Integer publishYear;
    private String indexStatus;
    private BigDecimal impactFactor;
    private String zone;
    private String abstractText;

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
