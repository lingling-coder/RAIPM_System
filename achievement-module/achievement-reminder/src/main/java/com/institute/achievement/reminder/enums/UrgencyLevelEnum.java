package com.institute.achievement.reminder.enums;

import lombok.Getter;

/**
 * Urgency level enum for reminder tasks (D-09).
 * <p>
 * Three levels with escalation timing metadata:
 * <ul>
 *   <li>{@link #HIGH} — Red: global popup + in-app + email. Escalate 7 days before deadline.</li>
 *   <li>{@link #MEDIUM} — Orange: in-app + email. Escalate 3 days before deadline.</li>
 *   <li>{@link #LOW} — Blue: in-app only. No escalation.</li>
 * </ul>
 *
 * @see com.institute.achievement.reminder.enums.ReminderTypeEnum
 */
@Getter
public enum UrgencyLevelEnum {

    HIGH("HIGH", "高", 7),
    MEDIUM("MEDIUM", "中", 3),
    LOW("LOW", "低", -1);

    /** Code stored in DB (HIGH / MEDIUM / LOW) */
    private final String code;

    /** Chinese label for frontend display */
    private final String label;

    /**
     * Days before deadline to start escalation.
     * <ul>
     *   <li>HIGH=7: escalate earlier — 7 days before deadline</li>
     *   <li>MEDIUM=3: escalate 3 days before deadline</li>
     *   <li>LOW=-1: no escalation (negative = disabled)</li>
     * </ul>
     */
    private final int escalationDaysBeforeDeadline;

    UrgencyLevelEnum(String code, String label, int escalationDaysBeforeDeadline) {
        this.code = code;
        this.label = label;
        this.escalationDaysBeforeDeadline = escalationDaysBeforeDeadline;
    }

    /**
     * Resolve an enum value from its code string.
     *
     * @param code the code string (e.g. "HIGH", "MEDIUM")
     * @return the matching enum, or {@code null} if no match found
     */
    public static UrgencyLevelEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (UrgencyLevelEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
