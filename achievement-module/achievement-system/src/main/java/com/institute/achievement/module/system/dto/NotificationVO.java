package com.institute.achievement.module.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * VO for notification display.
 */
@Data
public class NotificationVO {

    private Long id;
    private String type;
    private String title;
    private String content;
    private String relatedAchievementType;
    private Long relatedAchievementId;
    private Integer readFlag;
    private LocalDateTime createdTime;
}
