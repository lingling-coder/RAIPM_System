package com.institute.achievement.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * User-Role association entity (multi-role support per D-17).
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long roleId;
}
