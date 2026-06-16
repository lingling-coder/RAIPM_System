package com.institute.achievement.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Department entity.
 * Flat structure, no hierarchy per D-08.
 */
@Data
@TableName("sys_department")
public class SysDepartment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deptName;

    private String deptCode;

    private String leader;

    private String phone;

    /** 1=normal, 0=disabled */
    private Integer status;

    /** Soft delete flag */
    @TableLogic
    @TableField(select = false)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
