package com.institute.achievement.module.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * VO for approval record display.
 * Includes computed label fields for frontend rendering.
 */
@Data
public class ApprovalRecordVO {

    private Long id;
    private String action;
    private String actionLabel;
    private String operatorName;
    private String comment;
    private String fromStatus;
    private String fromStatusLabel;
    private String toStatus;
    private String toStatusLabel;
    private LocalDateTime createdTime;
}
