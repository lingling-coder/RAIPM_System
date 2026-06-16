package com.institute.achievement.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Approval record entity — maps to the {@code approval_record} table.
 * <p>
 * Stores every action taken during the approval workflow, including submit,
 * pass, reject, withdraw, and resubmit actions. Each record tracks the
 * operator, the status transition, and optional comments.
 */
@Data
@TableName("approval_record")
public class ApprovalRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String achievementType;
    private Long achievementId;
    private String action;
    private Long operatorId;
    private String operatorName;
    private String comment;
    private String fromStatus;
    private String toStatus;
    private LocalDateTime createdTime;
}
