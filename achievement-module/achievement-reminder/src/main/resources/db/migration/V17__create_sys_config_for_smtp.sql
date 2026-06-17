-- ============================================================================
-- V17: Create sys_config table for SMTP email settings
-- ============================================================================
-- Phase 4: Reminders & System Integration
-- Plan 04: SMTP email integration
--
-- D-14: SMTP config stored in sys_config table, runtime-dynamic, no restart
-- T-4-03: SMTP password AES-256 encrypted at rest
--
-- This table stores key-value configuration entries for the system.
-- Initially populated with SMTP settings for email delivery (API-02).
-- Can be extended by future phases for other system-level config entries.
-- ============================================================================

-- ── sys_config ──────────────────────────────────────────────────────────────
-- Generic key-value configuration table for system-level settings.
-- Each row represents a single configuration entry identified by a unique key.
CREATE TABLE IF NOT EXISTS `sys_config` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `config_key`   VARCHAR(100) NOT NULL                COMMENT 'Unique configuration key, e.g. smtp.host',
    `config_value` TEXT                                  COMMENT 'Configuration value (string/text)',
    `description`  VARCHAR(200)                          COMMENT 'Human-readable description of this config entry',
    `updated_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='System configuration key-value store — SMTP settings and future extensions';

-- ── Default SMTP Config Entries ─────────────────────────────────────────────
-- Insert default SMTP configuration values.
-- Password is left empty — admin must configure via the email config UI.
-- The password will be AES-256 encrypted by the application before storage.
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`) VALUES
    ('smtp.host',       '',       'SMTP server hostname, e.g. smtp.example.com'),
    ('smtp.port',       '587',    'SMTP server port (default 587 for STARTTLS, 465 for SSL)'),
    ('smtp.username',   '',       'SMTP authentication username (typically the email address)'),
    ('smtp.password',   '',       'SMTP authentication password (AES-256 encrypted at rest)'),
    ('smtp.senderName', '',       'Display name for the sender, e.g. 科研成果管理系统'),
    ('smtp.tls',        'true',   'Enable STARTTLS: true or false (default: true)')
ON DUPLICATE KEY UPDATE `config_key` = VALUES(`config_key`);
