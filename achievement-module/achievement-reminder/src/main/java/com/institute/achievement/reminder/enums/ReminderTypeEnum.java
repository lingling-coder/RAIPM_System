package com.institute.achievement.reminder.enums;

import lombok.Getter;

/**
 * Code-enumerated reminder types with embedded scheduling metadata (D-04).
 * <p>
 * Six reminder types covering all Phase 1 requirements. Each type has:
 * <ul>
 *   <li>code — DB-stored identifier matching {@code reminder_config.type_code}</li>
 *   <li>label — Chinese name for frontend display</li>
 *   <li>defaultAdvanceDays — default days before deadline to trigger reminder</li>
 *   <li>defaultUrgency — default urgency level (HIGH/MEDIUM/LOW)</li>
 *   <li>defaultTitleTemplate — default title with {achievementName} variable</li>
 *   <li>defaultBodyTemplate — default body with variable placeholders</li>
 * </ul>
 *
 * <p>Types are hardcoded in code, not DB-configurable in v1 (D-04). New types
 * are added via release updates.
 *
 * @see com.institute.achievement.reminder.enums.UrgencyLevelEnum
 */
@Getter
public enum ReminderTypeEnum {

    PROJECT_APPLICATION("PROJECT_APPLICATION", "项目申报", 30, UrgencyLevelEnum.MEDIUM,
            "项目申报提醒 — {achievementName}",
            "您好，{responsiblePerson}，{achievementName}项目申报截止日期为{deadline}，尚有{daysRemaining}天，请及时完成申报。"),

    AWARD_APPLICATION("AWARD_APPLICATION", "奖项申报", 30, UrgencyLevelEnum.MEDIUM,
            "奖项申报提醒 — {achievementName}",
            "您好，{responsiblePerson}，{achievementName}奖项申报截止日期为{deadline}，尚有{daysRemaining}天，请及时完成申报。"),

    PATENT_ANNUAL_FEE("PATENT_ANNUAL_FEE", "专利年费", 15, UrgencyLevelEnum.HIGH,
            "专利年费提醒 — {achievementName}",
            "您好，{responsiblePerson}，专利「{achievementName}」年费缴纳截止日期为{deadline}，尚有{daysRemaining}天，逾期将产生滞纳金。"),

    COPYRIGHT_MAINTENANCE("COPYRIGHT_MAINTENANCE", "软著维护", 30, UrgencyLevelEnum.LOW,
            "软著维护提醒 — {achievementName}",
            "您好，{responsiblePerson}，软著「{achievementName}」维护截止日期为{deadline}，尚有{daysRemaining}天，请及时处理。"),

    TRANSFORMATION_EVAL("TRANSFORMATION_EVAL", "转化后评估", 30, UrgencyLevelEnum.MEDIUM,
            "转化后评估提醒 — {achievementName}",
            "您好，{responsiblePerson}，{achievementName}转化后评估截止日期为{deadline}，尚有{daysRemaining}天，请及时填报效益数据。"),

    CLASSIFIED_AUDIT("CLASSIFIED_AUDIT", "涉密成果定期核查", 90, UrgencyLevelEnum.HIGH,
            "涉密成果核查提醒 — {achievementName}",
            "您好，{responsiblePerson}，涉密成果「{achievementName}」需于{deadline}前完成定期核查，尚有{daysRemaining}天。");

    /** Code stored in DB ({@code reminder_config.type_code}) */
    private final String code;

    /** Chinese label for frontend display */
    private final String label;

    /** Default days before deadline to trigger reminder */
    private final int defaultAdvanceDays;

    /** Default urgency level */
    private final UrgencyLevelEnum defaultUrgency;

    /** Default title template with {achievementName} variable */
    private final String defaultTitleTemplate;

    /** Default body template with variable placeholders */
    private final String defaultBodyTemplate;

    ReminderTypeEnum(String code, String label, int defaultAdvanceDays,
                     UrgencyLevelEnum defaultUrgency, String defaultTitleTemplate,
                     String defaultBodyTemplate) {
        this.code = code;
        this.label = label;
        this.defaultAdvanceDays = defaultAdvanceDays;
        this.defaultUrgency = defaultUrgency;
        this.defaultTitleTemplate = defaultTitleTemplate;
        this.defaultBodyTemplate = defaultBodyTemplate;
    }

    /**
     * Resolve an enum value from its code string.
     *
     * @param code the code string (e.g. "PROJECT_APPLICATION")
     * @return the matching enum, or {@code null} if no match found
     */
    public static ReminderTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ReminderTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
