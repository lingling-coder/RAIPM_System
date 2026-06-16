-- =====================================================================
-- V3: API Configuration Table
-- Implements D-34: DB-stored API config with online editor (no restart)
-- Provides centralized storage for external API integration settings
-- =====================================================================

CREATE TABLE api_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    config_name     VARCHAR(100) NOT NULL COMMENT '配置名称，如"Crossref API"',
    config_code     VARCHAR(50) NOT NULL UNIQUE COMMENT '配置编码，如"crossref_api"',
    endpoint_url    VARCHAR(500) NOT NULL COMMENT '接口地址',
    description     VARCHAR(500) COMMENT '描述',
    auth_type       VARCHAR(20) DEFAULT 'NONE' COMMENT '认证方式: NONE/API_KEY/BEARER/BASIC',
    api_key         VARCHAR(500) COMMENT 'API密钥(加密存储)',
    secret_key      VARCHAR(500) COMMENT '秘密密钥(加密存储)',
    token_url       VARCHAR(500) COMMENT 'OAuth2 Token URL',
    connect_timeout INT DEFAULT 5 COMMENT '连接超时(秒)',
    read_timeout    INT DEFAULT 10 COMMENT '读取超时(秒)',
    retry_count     INT DEFAULT 3 COMMENT '重试次数',
    retry_interval  INT DEFAULT 1 COMMENT '重试间隔(秒)',
    backoff_strategy VARCHAR(20) DEFAULT 'EXPONENTIAL' COMMENT '退避策略: EXPONENTIAL/FIXED',
    failure_alert   TINYINT DEFAULT 0 COMMENT '失败告警: 0=关闭, 1=开启',
    status          TINYINT DEFAULT 1 COMMENT '1=启用, 0=停用',
    last_test_time  DATETIME COMMENT '最后测试时间',
    last_test_result TINYINT COMMENT '最后测试结果: 1=成功, 0=失败',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by      VARCHAR(50) COMMENT '创建人',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    updated_by      VARCHAR(50) COMMENT '更新人'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API集成配置表';
