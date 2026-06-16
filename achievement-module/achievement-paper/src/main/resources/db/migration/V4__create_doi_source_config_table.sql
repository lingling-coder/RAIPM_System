-- V4: Create DOI source configuration table
-- Stores metadata about available DOI data sources and their health status.
-- Used by the admin configuration UI to manage source priority (D-11).
-- Actual priority order is read from application properties (doi.source.priority).
-- This table provides persistence for admin-configured overrides.

CREATE TABLE `doi_source_config` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `source_name`     VARCHAR(50)  NOT NULL                COMMENT '数据源名称(Crossref/OpenAlex)',
    `source_key`      VARCHAR(50)  NOT NULL                COMMENT '数据源标识(crossref/openalex)',
    `base_url`        VARCHAR(500) NOT NULL                COMMENT 'API基础URL',
    `priority_order`  INT          NOT NULL DEFAULT 0      COMMENT '优先级顺序(越小越优先)',
    `enabled`         TINYINT      NOT NULL DEFAULT 1      COMMENT '是否启用',
    `description`     VARCHAR(500) NULL                    COMMENT '描述信息',
    `created_by`      BIGINT       NOT NULL                COMMENT '创建人',
    `created_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`      BIGINT       NULL                    COMMENT '更新人',
    `updated_time`    DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_source_key` (`source_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='DOI数据源配置表';
