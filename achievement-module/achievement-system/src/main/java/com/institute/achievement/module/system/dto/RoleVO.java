package com.institute.achievement.module.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Role view object.
 */
@Data
public class RoleVO {

    private Long id;

    private String roleName;

    private String roleCode;

    private String description;

    /** 1=normal, 0=disabled */
    private Integer status;

    /** Number of users assigned this role */
    private Integer userCount;

    private LocalDateTime createdAt;
}
