package com.institute.achievement.patent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Patent achievement entity — maps to the {@code patent} table.
 * <p>
 * Contains all REG-02 metadata fields plus common lifecycle, audit,
 * and classification fields shared across all achievement types.
 */
@Data
@TableName("patent")
public class Patent {

    @TableId(type = IdType.AUTO)
    private Long id;

    // ── Core Metadata (REG-02) ──────────────────────────────────────

    private String patentName;
    private String inventors;
    private String applicationNo;
    private String authorizationNo;
    private LocalDate applicationDate;
    private LocalDate authorizationDate;
    private String patentType;
    private String country;
    private LocalDate nextFeeDate;
    private String legalStatus;

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
