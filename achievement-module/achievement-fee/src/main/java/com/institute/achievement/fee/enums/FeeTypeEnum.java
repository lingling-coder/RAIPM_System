package com.institute.achievement.fee.enums;

import lombok.Getter;

/**
 * Fee type enumeration — categorizes fee records by their nature.
 * <p>
 * Maps to the {@code fee_type} column in the {@code fee_record} table.
 *
 * <ul>
 *   <li>{@link #ANNUAL_FEE} — 专利年费 (annual patent renewal fee)</li>
 *   <li>{@link #REGISTRATION_FEE} — 登记费 (registration fee)</li>
 *   <li>{@link #MAINTENANCE_FEE} — 维护费 (maintenance fee)</li>
 *   <li>{@link #OTHER} — 其他 (miscellaneous fee)</li>
 * </ul>
 */
@Getter
public enum FeeTypeEnum {

    ANNUAL_FEE("annual_fee", "专利年费"),
    REGISTRATION_FEE("registration_fee", "登记费"),
    MAINTENANCE_FEE("maintenance_fee", "维护费"),
    OTHER("other", "其他");

    private final String code;
    private final String label;

    FeeTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * Resolve an enum value from its code string.
     *
     * @param code the code string (e.g. "annual_fee")
     * @return the matching enum, or {@code null} if no match found
     */
    public static FeeTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (FeeTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
