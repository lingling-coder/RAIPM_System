package com.institute.achievement.module.system.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User view object.
 * Excludes sensitive fields: password, lockout info, deleted flag.
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String realName;

    private String email;

    private String phone;

    private Long deptId;

    private String deptName;

    private List<Long> roleIds;

    private List<String> roleNames;

    /** 1=normal, 0=disabled */
    private Integer status;

    private String lastLoginIp;

    private LocalDateTime lastLoginTime;

    /** 1=force password change */
    private Integer passwordChangeRequired;

    private LocalDateTime createdAt;
}
