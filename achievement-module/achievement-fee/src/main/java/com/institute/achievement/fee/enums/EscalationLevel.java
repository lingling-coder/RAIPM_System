package com.institute.achievement.fee.enums;

import lombok.Getter;

import java.time.Duration;

/**
 * Escalation level state machine for alert escalation workflow (D-24).
 * <p>
 * Defines the 3-tier escalation path for unresolved fee alerts:
 * <ol>
 *   <li>{@link #FIRST_ALERT} — Alert initially sent to the responsible person</li>
 *   <li>{@link #DEPT_HEAD} — Escalated to department head after 3 days of no resolution</li>
 *   <li>{@link #LEADERSHIP} — Escalated to leadership after 8 days total (3 + 5)</li>
 * </ol>
 *
 * <h3>State Machine</h3>
 * <pre>
 *   FIRST_ALERT --[3d]--> DEPT_HEAD --[5d]--> LEADERSHIP
 * </pre>
 *
 * @see com.institute.achievement.fee.entity.AlertRecord
 * @see com.institute.achievement.fee.scheduler.AlertEscalationTask
 */
@Getter
public enum EscalationLevel {

    NONE("NONE", "未升级", Duration.ZERO),
    FIRST_ALERT("FIRST_ALERT", "首次预警", Duration.ofDays(0)),
    DEPT_HEAD("DEPT_HEAD", "部门负责人", Duration.ofDays(3)),
    LEADERSHIP("LEADERSHIP", "院领导", Duration.ofDays(8)); // 3 + 5

    /** Code stored in DB (NONE / FIRST_ALERT / DEPT_HEAD / LEADERSHIP) */
    private final String code;

    /** Chinese label for logs and display */
    private final String label;

    /**
     * The threshold duration (in days) since alert trigger at which this
     * escalation level activates.
     */
    private final Duration threshold;

    EscalationLevel(String code, String label, Duration threshold) {
        this.code = code;
        this.label = label;
        this.threshold = threshold;
    }

    /**
     * Resolve an enum value from its code string.
     *
     * @param code the code string (e.g. "DEPT_HEAD", "LEADERSHIP")
     * @return the matching enum, or {@code null} if no match found
     */
    public static EscalationLevel fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (EscalationLevel value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Determine the next escalation level based on hours since the alert was triggered.
     * <p>
     * State machine:
     * <ul>
     *   <li>&lt; 72 hours (3 days): stay at current level (no escalation)</li>
     *   <li>&ge; 72 hours (3 days) AND current is NONE/FIRST_ALERT: escalate to DEPT_HEAD</li>
     *   <li>&ge; 192 hours (8 days) AND current != LEADERSHIP: escalate to LEADERSHIP</li>
     * </ul>
     *
     * @param currentLevelCode the current escalation level code from the alert record
     * @param hoursSinceTrigger the number of hours since the alert was triggered
     * @return the target escalation level, or {@code null} if no escalation is needed
     */
    public static EscalationLevel determineNextLevel(String currentLevelCode, long hoursSinceTrigger) {
        if (hoursSinceTrigger >= 192 && !LEADERSHIP.getCode().equals(currentLevelCode)) {
            return LEADERSHIP;
        }
        if (hoursSinceTrigger >= 72 && (NONE.getCode().equals(currentLevelCode)
                || FIRST_ALERT.getCode().equals(currentLevelCode))) {
            return DEPT_HEAD;
        }
        return null; // No escalation needed yet
    }
}
