package com.institute.achievement.fee.enums;

import lombok.Getter;

/**
 * Fee plan status enumeration.
 * <p>
 * Maps to the {@code status} column in the {@code fee_plan} table.
 *
 * <ul>
 *   <li>{@link #ACTIVE} — 启用中 (active, generating alerts)</li>
 *   <li>{@link #PAUSED} — 已暂停 (paused, e.g. patent invalidated)</li>
 * </ul>
 */
@Getter
public enum FeePlanStatusEnum {

    ACTIVE("active", "启用中"),
    PAUSED("paused", "已暂停");

    private final String code;
    private final String label;

    FeePlanStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * Resolve an enum value from its code string.
     *
     * @param code the code string (e.g. "active")
     * @return the matching enum, or {@code null} if no match found
     */
    public static FeePlanStatusEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (FeePlanStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
