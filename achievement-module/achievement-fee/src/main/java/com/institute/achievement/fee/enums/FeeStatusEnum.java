package com.institute.achievement.fee.enums;

import lombok.Getter;

/**
 * Fee payment status enumeration.
 * <p>
 * Maps to the {@code status} column in the {@code fee_record} table.
 *
 * <ul>
 *   <li>{@link #PENDING} — 待缴费 (awaiting payment)</li>
 *   <li>{@link #PAID} — 已缴费 (paid)</li>
 *   <li>{@link #PAUSED} — 已暂停 (paused, e.g. patent invalidated)</li>
 * </ul>
 */
@Getter
public enum FeeStatusEnum {

    PENDING("pending", "待缴费"),
    PAID("paid", "已缴费"),
    PAUSED("paused", "已暂停");

    private final String code;
    private final String label;

    FeeStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * Resolve an enum value from its code string.
     *
     * @param code the code string (e.g. "pending")
     * @return the matching enum, or {@code null} if no match found
     */
    public static FeeStatusEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (FeeStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
