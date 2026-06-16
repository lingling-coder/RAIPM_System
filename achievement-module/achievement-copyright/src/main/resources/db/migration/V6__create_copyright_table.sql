-- V6: Create software copyright table
-- Core table for software copyright achievement registration with all REG-03 fields.
-- Supports the full lifecycle with version-based optimistic locking.

CREATE TABLE `software_copyright` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    -- Core metadata fields (REG-03)
    `name`              VARCHAR(500) NOT NULL                COMMENT '软著名称',
    `copyright_holder`  VARCHAR(500) NOT NULL                COMMENT '著作权人',
    `registration_no`   VARCHAR(100) NOT NULL                COMMENT '登记号',
    `registration_date` DATE         NOT NULL                COMMENT '登记日期',
    `software_version`  VARCHAR(100) NOT NULL                COMMENT '版本号',
    `software_category` VARCHAR(50)  NOT NULL                COMMENT '软件类别(操作系统/数据库/中间件/应用软件/嵌入式软件/其他)',

    -- Classification (D-06)
    `is_classified`     TINYINT      NOT NULL DEFAULT 0      COMMENT '涉密标记(0=否, 1=是)',
    `classified_level`  VARCHAR(20) NULL                     COMMENT '密级(秘密/机密)',

    -- Project linkage (D-08)
    `project_ref`       VARCHAR(500) NULL                    COMMENT '所属课题(自由文本)',

    -- Lifecycle fields
    `status`            VARCHAR(50)  NOT NULL DEFAULT 'DRAFT' COMMENT '成果状态',
    `archive_no`        VARCHAR(100) NULL                    COMMENT '归档号/成果编号',

    -- Department & ownership (for SQL-layer permission)
    `dept_id`           BIGINT       NOT NULL                COMMENT '所属部门ID',
    `created_by`        BIGINT       NOT NULL                COMMENT '创建人ID',
    `created_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`        BIGINT       NULL                    COMMENT '更新人ID',
    `updated_time`      DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- Optimistic locking (T-01-02-02 mitigation)
    `version`           INT          NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',

    PRIMARY KEY (`id`),

    -- Unique index on registration_no (REG-09 duplicate detection)
    UNIQUE INDEX `idx_registration_no` (`registration_no`),

    -- Query performance indexes
    INDEX `idx_status` (`status`),
    INDEX `idx_dept_id` (`dept_id`),
    INDEX `idx_created_time` (`created_time`),

    -- Draft query support: user's drafts
    INDEX `idx_draft_user` (`created_by`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='软件著作权成果表';
