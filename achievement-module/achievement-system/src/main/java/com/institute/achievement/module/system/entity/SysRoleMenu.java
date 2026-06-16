package com.institute.achievement.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * Role-Menu permission mapping entity (RBAC).
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roleId;

    private Long menuId;
}
