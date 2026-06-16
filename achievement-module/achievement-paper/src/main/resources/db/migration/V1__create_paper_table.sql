-- V1: Create paper table
-- Core table for paper achievement registration with all REG-01 fields.
-- Supports the full lifecycle from draft to archived with version-based optimistic locking.

CREATE TABLE `paper` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    -- Core metadata fields (REG-01)
    `title`           VARCHAR(500) NOT NULL                COMMENT '论文标题',
    `authors`         VARCHAR(1000) NOT NULL               COMMENT '作者(分号分隔)',
    `journal`         VARCHAR(500) NOT NULL                COMMENT '期刊/会议名称',
    `doi`             VARCHAR(255) NULL                    COMMENT 'DOI标识符',
    `issn`            VARCHAR(50) NULL                     COMMENT 'ISSN/CN',
    `volume`          INT          NULL                    COMMENT '卷号',
    `issue`           INT          NULL                    COMMENT '期号',
    `pages`           VARCHAR(50) NULL                     COMMENT '页码(如: 123-130)',
    `publish_year`    YEAR         NOT NULL                COMMENT '发表年份',
    `index_status`    VARCHAR(50) NOT NULL                 COMMENT '收录情况(SCI/SSCI/EI/CPCI/CSCD/CSSCI/北大核心/其他)',
    `impact_factor`   DECIMAL(10,3) NULL                   COMMENT '影响因子',
    `zone`            VARCHAR(20) NULL                     COMMENT '中科院分区(一区/二区/三区/四区/无)',
    `abstract_text`   TEXT         NULL                    COMMENT '摘要',

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

    -- Optimistic locking (T-01-02 mitigation)
    `version`         INT          NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',

    PRIMARY KEY (`id`),

    -- Unique index on DOI (only when not null)
    UNIQUE INDEX `idx_doi` (`doi`),

    -- Query performance indexes
    INDEX `idx_status` (`status`),
    INDEX `idx_dept_id` (`dept_id`),
    INDEX `idx_created_time` (`created_time`),
    INDEX `idx_created_by` (`created_by`),

    -- Draft query support: user's drafts
    INDEX `idx_draft_user` (`created_by`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='论文成果表';
