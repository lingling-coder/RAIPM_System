package com.institute.achievement.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data dictionary entry entity.
 * Right-side table entries in the dictionary management UI (D-10).
 */
@Data
@TableName("sys_dict_entry")
public class SysDictEntry {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String dictKey;

    private String dictValue;

    private Integer sortOrder;

    /** 1=normal, 0=disabled */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
