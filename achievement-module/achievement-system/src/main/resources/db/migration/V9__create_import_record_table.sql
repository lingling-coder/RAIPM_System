-- Import record table for batch import tracking.
-- Stores metadata about each batch import operation including file name,
-- row counters, and error report path.
--
-- Requirements: REG-05 (Excel batch import)
-- Decisions: D-16 (direct import with result report), D-18 (partial import)

CREATE TABLE IF NOT EXISTS `import_record` (
    `id`              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    `importer_id`     BIGINT       NOT NULL COMMENT '导入用户ID',
    `file_name`       VARCHAR(500) NOT NULL COMMENT '导入文件名',
    `total_rows`      INT          NOT NULL DEFAULT 0 COMMENT '总行数',
    `success_rows`    INT          NOT NULL DEFAULT 0 COMMENT '成功行数',
    `error_rows`      INT          NOT NULL DEFAULT 0 COMMENT '失败行数',
    `skipped_rows`    INT          NOT NULL DEFAULT 0 COMMENT '跳过行数',
    `error_report_path` VARCHAR(1000) NULL COMMENT '错误报告文件路径',
    `created_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
    INDEX `idx_importer` (`importer_id`),
    INDEX `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批量导入记录表';
