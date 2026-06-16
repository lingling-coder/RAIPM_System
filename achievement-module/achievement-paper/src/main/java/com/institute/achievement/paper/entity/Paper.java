package com.institute.achievement.paper.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Paper achievement entity — maps to the {@code paper} table.
 * <p>
 * Contains all REG-01 metadata fields plus common lifecycle, audit,
 * and classification fields shared across all achievement types.
 */
@Data
@TableName("paper")
public class Paper {

    @TableId(type = IdType.AUTO)
    private Long id;

    // ── Core Metadata (REG-01) ──────────────────────────────────────

    private String title;
    private String authors;
    private String journal;
    private String doi;
    private String issn;
    private Integer volume;
    private Integer issue;
    private String pages;
    private Integer publishYear;
    private String indexStatus;
    private java.math.BigDecimal impactFactor;
    private String zone;
    private String abstractText;

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
