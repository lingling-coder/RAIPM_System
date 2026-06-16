package com.institute.achievement.fee.enums;

import lombok.Getter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 4-tier alert level enumeration for fee payment warnings (D-22).
 * <p>
 * Classification is based on days until the fee due date:
 * <ul>
 *   <li>{@link #BLUE} — 30 days before due: "即将缴费"</li>
 *   <li>{@link #YELLOW} — 15 days before due: "请尽快缴费"</li>
 *   <li>{@link #ORANGE} — 7 days before due: "截止在即"</li>
 *   <li>{@link #RED} — overdue: "已逾期"</li>
 * </ul>
 *
 * @see com.institute.achievement.fee.entity.AlertRecord
 */
@Getter
public enum AlertLevelEnum {

    BLUE("BLUE", "即将缴费", 30),
    YELLOW("YELLOW", "请尽快缴费", 15),
    ORANGE("ORANGE", "截止在即", 7),
    RED("RED", "已逾期", 0);

    /** Code stored in DB (BLUE / YELLOW / ORANGE / RED) */
    private final String code;

    /** Chinese label for frontend display */
    private final String label;

    /**
     * Days before due date when this alert triggers.
     * BLUE=30, YELLOW=15, ORANGE=7, RED=0 (overdue).
     */
    private final int daysBeforeDue;

    AlertLevelEnum(String code, String label, int daysBeforeDue) {
        this.code = code;
        this.label = label;
        this.daysBeforeDue = daysBeforeDue;
    }

    /**
     * Classify a fee due date into an alert level based on today's date.
     *
     * @param dueDate the fee due date (nullable — returns null if null)
     * @return the matching alert level, or {@code null} if no alert is needed
     *         (more than 30 days until due)
     */
    public static AlertLevelEnum fromDueDate(LocalDate dueDate) {
        if (dueDate == null) {
            return null;
        }

        LocalDate today = LocalDate.now();
        long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);

        if (daysUntilDue < 0) {
            return RED; // overdue
        } else if (daysUntilDue <= 7) {
            return ORANGE;
        } else if (daysUntilDue <= 15) {
            return YELLOW;
        } else if (daysUntilDue <= 30) {
            return BLUE;
        }

        return null; // more than 30 days away — no alert needed
    }

    /**
     * Resolve an enum value from its code string.
     *
     * @param code the code string (e.g. "BLUE", "RED")
     * @return the matching enum, or {@code null} if no match found
     */
    public static AlertLevelEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AlertLevelEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Get the CSS class name for frontend color mapping.
     *
     * @param code the alert level code
     * @return the CSS class name, or empty string if unknown
     */
    public static String getColorClass(String code) {
        if (code == null) {
            return "";
        }
        switch (code) {
            case "BLUE":
                return "alert-blue";
            case "YELLOW":
                return "alert-yellow";
            case "ORANGE":
                return "alert-orange";
            case "RED":
                return "alert-red";
            default:
                return "";
        }
    }
}
