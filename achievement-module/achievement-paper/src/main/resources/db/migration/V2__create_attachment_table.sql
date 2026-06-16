-- V2: Create attachment table
-- Stores metadata for uploaded files attached to achievements.
-- Uses Exclusive Arc pattern (P0-01): exactly one of paper_id/patent_id/copyright_id is non-null.
-- File content is stored on local filesystem via the file proxy service (P0-06).

CREATE TABLE `attachment` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    -- File metadata
    `original_name`   VARCHAR(500) NOT NULL                COMMENT '原始文件名',
    `stored_name`     VARCHAR(255) NOT NULL                COMMENT 'UUID存储名(代理文件服务生成)',
    `file_path`       VARCHAR(1000) NOT NULL               COMMENT '存储路径(相对uploads目录)',
    `file_size`       BIGINT       NOT NULL                COMMENT '文件大小(字节)',
    `file_type`       VARCHAR(100) NOT NULL                COMMENT 'MIME类型',

    -- Exclusive Arc: exactly one of these is non-null per P0-01
    `paper_id`        BIGINT       NULL                    COMMENT '关联论文ID',
    `patent_id`       BIGINT       NULL                    COMMENT '关联专利ID',
    `copyright_id`    BIGINT       NULL                    COMMENT '关联软著ID',

    -- Audit fields
    `uploader_id`     BIGINT       NOT NULL                COMMENT '上传人ID',
    `upload_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',

    -- Soft delete
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '删除标记(0=未删, 1=已删)',
    `deleted_time`    DATETIME     NULL                    COMMENT '删除时间',

    PRIMARY KEY (`id`),

    -- Query indexes
    INDEX `idx_paper_id` (`paper_id`),
    INDEX `idx_patent_id` (`patent_id`),
    INDEX `idx_copyright_id` (`copyright_id`),
    INDEX `idx_uploader` (`uploader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='附件表';
