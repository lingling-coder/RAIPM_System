package com.institute.achievement.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * System user entity.
 * Supports enable/disable (D-19), soft delete (D-20), account lockout (D-15).
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    private String email;

    private String phone;

    private Long deptId;

    /** 1=normal, 0=disabled */
    private Integer status;

    /** Soft delete flag: 1=deleted, 0=active */
    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    /** Account lockout expiration timestamp (null = not locked) */
    private LocalDateTime lockoutUntil;

    /** Consecutive login failures counter */
    private Integer loginFailures;

    private String lastLoginIp;

    private LocalDateTime lastLoginTime;

    /** 1=force password change on next login */
    private Integer passwordChangeRequired;

    /** Authentication source: LOCAL, LDAP, OAUTH */
    private String authSource;

    /** External system reference ID for SSO/LDAP mapping */
    private String externalRefId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
