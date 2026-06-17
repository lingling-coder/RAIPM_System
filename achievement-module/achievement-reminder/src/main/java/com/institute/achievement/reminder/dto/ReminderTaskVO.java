package com.institute.achievement.reminder.dto;

import com.institute.achievement.reminder.enums.ReminderTypeEnum;
import com.institute.achievement.reminder.enums.UrgencyLevelEnum;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * View object for reminder task display.
 * <p>
 * Combines entity fields with resolved display names from related tables
 * and enums. Includes recalculated {@link #daysUntilDeadline} at query time.
 */
@Data
public class ReminderTaskVO {

    private Long id;
    private Long configId;
    private Long userId;
    private String achievementName;
    private String title;
    private String content;
    private LocalDate deadline;
    private Integer daysRemaining;
    private String urgency;
    private Integer confirmedFlag;
    private LocalDateTime confirmedTime;
    private String escalationLevel;
    private LocalDateTime escalationTime;
    private Integer emailSentFlag;
    private LocalDateTime emailSentTime;
    private Long deptId;
    private LocalDateTime createdTime;

    // ── Resolved fields ──

    /** Type code resolved from config */
    private String typeCode;

    /** Chinese type name from ReminderTypeEnum */
    private String typeName;

    /** User's display name */
    private String userName;

    /** Department name */
    private String deptName;

    /** Config title */
    private String configTitle;

    /** Chinese urgency label from UrgencyLevelEnum */
    private String urgencyLabel;

    /** Recalculated days until deadline at query time */
    private Long daysUntilDeadline;

    /**
     * Recalculate daysUntilDeadline from current date.
     * Called after loading from DB to ensure the count is current.
     */
    public void recalcDaysUntilDeadline() {
        if (deadline != null) {
            this.daysUntilDeadline = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
        }
    }

    /**
     * Resolve the Chinese urgency label from the urgency code.
     */
    public void resolveUrgencyLabel() {
        if (urgency != null) {
            UrgencyLevelEnum level = UrgencyLevelEnum.fromCode(urgency);
            this.urgencyLabel = level != null ? level.getLabel() : urgency;
        }
    }

    /**
     * Resolve type name from the type code.
     */
    public void resolveTypeName() {
        if (typeCode != null) {
            ReminderTypeEnum type = ReminderTypeEnum.fromCode(typeCode);
            this.typeName = type != null ? type.getLabel() : typeCode;
        }
    }
}
