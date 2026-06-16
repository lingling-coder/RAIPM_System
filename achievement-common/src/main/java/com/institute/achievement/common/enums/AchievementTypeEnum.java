package com.institute.achievement.common.enums;

import lombok.Getter;

/**
 * Enumeration of achievement types supported by the system.
 * Uses exclusive-arc pattern for polymorphic associations throughout the domain.
 */
@Getter
public enum AchievementTypeEnum {

    PAPER("论文"),
    PATENT("专利"),
    COPYRIGHT("软件著作权");

    private final String displayName;

    AchievementTypeEnum(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Resolve from a code string (case-insensitive).
     */
    public static AchievementTypeEnum fromCode(String code) {
        for (AchievementTypeEnum type : values()) {
            if (type.name().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown achievement type: " + code);
    }
}
