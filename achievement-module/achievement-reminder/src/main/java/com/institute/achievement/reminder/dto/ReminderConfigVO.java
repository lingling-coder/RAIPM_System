package com.institute.achievement.reminder.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * View object for reminder configuration — extends DTO with resolved display fields.
 * <p>
 * Includes the Chinese type name resolved from {@code ReminderTypeEnum},
 * responsible person/role names, and computed deadline for frontend display.
 */
@Data
public class ReminderConfigVO {

    private Long id;

    /** Reminder type code — maps to ReminderTypeEnum.code */
    private String typeCode;

    /** Chinese label resolved from ReminderTypeEnum */
    private String typeName;

    /** Achievement name — entered by admin */
    private String achievementName;

    /** Title template with {achievementName} variable */
    private String titleTemplate;

    /** Body template with variable placeholders */
    private String bodyTemplate;

    /** Urgency level: HIGH / MEDIUM / LOW */
    private String urgency;

    /** Days before deadline to trigger reminder */
    private Integer advanceDays;

    /** Explicit deadline date (nullable) */
    private LocalDate deadline;

    /** Computed deadline: explicit deadline if set, otherwise today + advanceDays */
    private LocalDate computedDeadline;

    /** Cron expression or scheduling rule (optional) */
    private String schedulingRule;

    /** Primary responsible person (user ID) */
    private Long responsibleUserId;

    /** Responsible user name (resolved from user service) */
    private String responsibleUserName;

    /** Role code for role-based fallback */
    private String responsibleRoleCode;

    /** Role name (resolved from role code) */
    private String responsibleRoleName;

    /** Status: 1=enabled, 0=disabled */
    private Integer status;

    /** Department ID */
    private Long deptId;

    /** Creation timestamp */
    private LocalDateTime createdTime;

    /** Last update timestamp */
    private LocalDateTime updatedTime;
}
