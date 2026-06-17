package com.institute.achievement.reminder.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO for creating/updating a reminder configuration.
 * <p>
 * Maps to the {@code reminder_config} table fields with jakarta.validation
 * constraints enforced at the controller layer (T-4-02).
 */
@Data
public class ReminderConfigDTO {

    private Long id;

    /** Reminder type code — maps to ReminderTypeEnum.code */
    @NotBlank(message = "提醒类型不能为空")
    private String typeCode;

    /** Achievement name — entered by admin per config-driven data model */
    @NotBlank(message = "成果名称不能为空")
    @Size(max = 200, message = "成果名称不能超过200个字符")
    private String achievementName;

    /** Title template with {achievementName} variable */
    @NotBlank(message = "标题模板不能为空")
    @Size(max = 200, message = "标题模板不能超过200个字符")
    private String titleTemplate;

    /** Body template with variable placeholders ({achievementName}/{deadline}/{daysRemaining}/{responsiblePerson}) */
    private String bodyTemplate;

    /** Urgency level: HIGH / MEDIUM / LOW */
    @NotBlank(message = "紧急等级不能为空")
    private String urgency;

    /** Days before deadline to trigger reminder — deadline = today + advanceDays */
    @Min(value = 1, message = "提前天数不能小于1")
    @Max(value = 365, message = "提前天数不能超过365")
    private Integer advanceDays;

    /** Explicit deadline date — overrides advanceDays when set */
    private LocalDate deadline;

    /** Cron expression or scheduling rule (optional) */
    private String schedulingRule;

    /** Primary responsible person (user ID) */
    private Long responsibleUserId;

    /** Role code for role-based fallback (e.g. ROLE_SECRETARY) */
    private String responsibleRoleCode;

    /** Status: 1=enabled, 0=disabled */
    private Integer status;
}
