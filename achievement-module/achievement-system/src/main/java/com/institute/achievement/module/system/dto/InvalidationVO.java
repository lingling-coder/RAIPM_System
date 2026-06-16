package com.institute.achievement.module.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * VO for invalidation record display.
 * <p>
 * Used by InvalidationController responses to return structured
 * invalidation history to the frontend.
 */
@Data
public class InvalidationVO {

    private Long id;
    private String achievementType;
    private Long achievementId;
    private String invalidatorName;
    private String reason;
    private LocalDateTime createdTime;
}
