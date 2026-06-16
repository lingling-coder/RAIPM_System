package com.institute.achievement.module.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Department view object.
 */
@Data
public class DepartmentVO {

    private Long id;

    private String deptName;

    private String deptCode;

    private String leader;

    private String phone;

    /** 1=normal, 0=disabled */
    private Integer status;

    /** Number of members in this department */
    private Integer memberCount;

    private LocalDateTime createdAt;
}
