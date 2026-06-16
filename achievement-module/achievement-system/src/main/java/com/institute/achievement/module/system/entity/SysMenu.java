package com.institute.achievement.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Menu/Permission entity for RBAC.
 * Supports tree structure via parent_id self-reference.
 * type: 0=directory, 1=menu, 2=button (permission).
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Parent menu ID (0 = root) */
    private Long parentId;

    /** Menu display name */
    private String name;

    /** Permission identifier e.g. "system:user:list" */
    private String permission;

    /** Route path for Vue Router */
    private String path;

    /** Vue component path */
    private String component;

    /** 0=directory, 1=menu, 2=button */
    private Integer type;

    /** Element Plus icon name */
    private String icon;

    private Integer sortOrder;

    /** 1=visible, 0=hidden */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
