package com.institute.achievement.copyright.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Software copyright achievement entity — maps to the {@code software_copyright} table.
 * <p>
 * Contains all REG-03 metadata fields plus common lifecycle, audit,
 * and classification fields shared across all achievement types.
 */
@Data
@TableName("software_copyright")
public class Copyright {

    @TableId(type = IdType.AUTO)
    private Long id;

    // ── Core Metadata (REG-03) ──────────────────────────────────────

    private String name;
    private String copyrightHolder;
    private String registrationNo;
    private LocalDate registrationDate;
    private String softwareVersion;
    private String softwareCategory;

    // ── Classification (D-06) ───────────────────────────────────────

    private Integer isClassified;
    private String classifiedLevel;

    // ── Project Linkage (D-08) ──────────────────────────────────────

    private String projectRef;

    // ── Lifecycle ───────────────────────────────────────────────────

    private String status;
    private String archiveNo;

    // ── Department & Ownership ──────────────────────────────────────

    private Long deptId;
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    private Long updatedBy;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedTime;

    // ── Optimistic Locking ──────────────────────────────────────────

    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;
}
