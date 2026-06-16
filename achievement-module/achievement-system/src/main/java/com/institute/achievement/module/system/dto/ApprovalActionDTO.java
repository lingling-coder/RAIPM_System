package com.institute.achievement.module.system.dto;

import lombok.Data;

/**
 * DTO for approval action requests.
 * Used by the ApprovalController for approve/reject operations.
 */
@Data
public class ApprovalActionDTO {

    /** Achievement type: paper, patent, or copyright */
    private String achievementType;

    /** Achievement ID */
    private Long achievementId;

    /** Action: APPROVE or REJECT */
    private String action;

    /** Comment/reason (required for reject) */
    private String comment;

    /** Archive number (required for admin archive) */
    private String archiveNo;
}
