package com.institute.achievement.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Notification entity — maps to the {@code notification} table.
 * <p>
 * Stores in-app notifications for approval tasks and system messages.
 * Unread count is cached in Redis for fast badge display.
 */
@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String type;
    private String title;
    private String content;
    private String relatedAchievementType;
    private Long relatedAchievementId;
    private Integer readFlag;
    private LocalDateTime createdTime;
}
