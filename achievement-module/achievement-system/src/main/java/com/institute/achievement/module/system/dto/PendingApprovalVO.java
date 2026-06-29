package com.institute.achievement.module.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Pending approval item VO for the secretary/admin approval list.
 * <p>
 * Contains the achievement display info along with resolved
 * submitter and department names for frontend rendering.
 */
@Data
public class PendingApprovalVO {

    /** Achievement ID */
    private Long id;

    /** Achievement type: paper / patent / copyright */
    private String type;

    /** Achievement title (paper.title / patent.patent_name / copyright.name) */
    private String title;

    /** Submitter's display name (real_name from sys_user) */
    private String submitterName;

    /** Department name (dept_name from sys_department) */
    private String deptName;

    /** Submission timestamp (created_time of the achievement) */
    private LocalDateTime submitTime;

    // ── Internal fields for name resolution (not exposed to frontend) ──

    /** Creator user ID (used for name resolution) */
    private Long createdBy;

    /** Department ID (used for dept name resolution) */
    private Long deptId;
}
