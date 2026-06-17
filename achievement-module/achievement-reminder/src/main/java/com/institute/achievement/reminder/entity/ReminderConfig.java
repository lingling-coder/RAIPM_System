package com.institute.achievement.reminder.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Reminder configuration entity — maps to the {@code reminder_config} table (D-03).
 * <p>
 * Stores admin-defined reminder type configuration including template text,
 * scheduling rules, urgency level, and responsible person assignment.
 * Each configuration can produce many {@link ReminderTask} instances via
 * the daily scheduler.
 *
 * <h3>Data sources (Phase 1)</h3>
 * <ul>
 *   <li>{@code achievementName} — manually entered by admin when creating config</li>
 *   <li>{@code deadline} — nullable; when null, {@code advanceDays} is used to
 *       compute {@code deadline = today + advanceDays}</li>
 * </ul>
 *
 * @see com.institute.achievement.reminder.enums.ReminderTypeEnum
 * @see ReminderTask
 */
@Data
@TableName("reminder_config")
public class ReminderConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Reminder type code (maps to ReminderTypeEnum.code) */
    @TableField("type_code")
    private String typeCode;

    /** Achievement name — entered by admin; used for template variable {achievementName} substitution */
    @TableField("achievement_name")
    private String achievementName;

    /** Title template with {achievementName} variable */
    @TableField("title_template")
    private String titleTemplate;

    /** Body template with variable placeholders */
    @TableField("body_template")
    private String bodyTemplate;

    /** Urgency level: HIGH / MEDIUM / LOW */
    private String urgency;

    /** Days before deadline to trigger reminder — deadline = today + advance_days */
    @TableField("advance_days")
    private Integer advanceDays;

    /** Explicit deadline date — overrides advance_days calculation when set */
    private LocalDate deadline;

    /** Cron expression or scheduling rule (optional) */
    @TableField("scheduling_rule")
    private String schedulingRule;

    /** Primary responsible person (user ID) */
    @TableField("responsible_user_id")
    private Long responsibleUserId;

    /** Role code for role-based routing (e.g. ROLE_SECRETARY) */
    @TableField("responsible_role_code")
    private String responsibleRoleCode;

    /** Status: 1=enabled, 0=disabled */
    private Integer status;

    /** Department ID (for SQL-layer data isolation) */
    @TableField("dept_id")
    private Long deptId;

    /** Creator user ID */
    @TableField("created_by")
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /** Last updater user ID */
    @TableField("updated_by")
    private Long updatedBy;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedTime;
}
