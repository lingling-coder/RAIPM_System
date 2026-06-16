-- V5: Create patent table
-- Core table for patent achievement registration with all REG-02 fields.
-- Supports the full lifecycle from draft to archived with version-based optimistic locking.

CREATE TABLE `patent` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    -- Core metadata fields (REG-02)
    `patent_name`     VARCHAR(500) NOT NULL                COMMENT '专利名称',
    `inventors`       VARCHAR(1000) NOT NULL               COMMENT '发明人(分号分隔)',
    `application_no`  VARCHAR(100) NOT NULL                COMMENT '专利申请号',
    `authorization_no` VARCHAR(100) NULL                   COMMENT '授权号',
    `application_date` DATE        NOT NULL                COMMENT '申请日',
    `authorization_date` DATE      NULL                    COMMENT '授权日',
    `patent_type`     VARCHAR(50)  NOT NULL                COMMENT '专利类型(发明/实用新型/外观设计)',
    `country`         VARCHAR(50)  NOT NULL DEFAULT '中国'  COMMENT '国别',
    `next_fee_date`   DATE         NULL                    COMMENT '年费下次缴费日',
    `legal_status`    VARCHAR(50)  NOT NULL                COMMENT '法律状态(授权/实审/公开/驳回/撤回/终止/无效)',

    -- Classification (D-06)
    `is_classified`   TINYINT      NOT NULL DEFAULT 0      COMMENT '涉密标记(0=否, 1=是)',
    `classified_level` VARCHAR(20) NULL                    COMMENT '密级(秘密/机密)',

    -- Project linkage (D-08)
    `project_ref`     VARCHAR(500) NULL                    COMMENT '所属课题(自由文本)',

    -- Lifecycle fields
    `status`          VARCHAR(50)  NOT NULL DEFAULT 'DRAFT' COMMENT '成果状态',
    `archive_no`      VARCHAR(100) NULL                    COMMENT '归档号/成果编号',

    -- Department & ownership (for SQL-layer permission)
    `dept_id`         BIGINT       NOT NULL                COMMENT '所属部门ID',
    `created_by`      BIGINT       NOT NULL                COMMENT '创建人ID',
    `created_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`      BIGINT       NULL                    COMMENT '更新人ID',
    `updated_time`    DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- Optimistic locking (T-01-02-01 mitigation)
    `version`         INT          NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',

    PRIMARY KEY (`id`),

    -- Unique index on application_no (REG-09 duplicate detection)
    UNIQUE INDEX `idx_application_no` (`application_no`),

    -- Query performance indexes
    INDEX `idx_status` (`status`),
    INDEX `idx_dept_id` (`dept_id`),
    INDEX `idx_created_time` (`created_time`),

    -- Draft query support: user's drafts
    INDEX `idx_draft_user` (`created_by`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='专利成果表';
