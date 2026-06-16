-- V12: Create fee_plan table
-- Payment plan engine for patents (FEE-02).
-- Supports both manually created one-time fee plans and automatically
-- generated recurring annual fee plans from patent authorizationDate.
-- Plan lifecycle: active -> paused (on invalidation) -> deleted (paused only).

CREATE TABLE `fee_plan` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    -- Patent link (D-14: plans are always for patents)
    `patent_id`         BIGINT       NOT NULL                COMMENT '关联专利ID',

    -- Fee type
    `fee_type`          VARCHAR(50)  NOT NULL                COMMENT '费用类型(annual_fee/registration_fee/maintenance_fee/other)',

    -- Amount
    `amount`            DECIMAL(12,2) NOT NULL               COMMENT '金额(元)',

    -- Due date (system-locked per D-17)
    `due_date`          DATE         NOT NULL                COMMENT '缴费截止日期',

    -- Status
    `status`            VARCHAR(20)  NOT NULL DEFAULT 'active'  COMMENT '计划状态(active/paused)',

    -- Source
    `source`            VARCHAR(20)  NOT NULL DEFAULT 'auto_generated' COMMENT '来源(auto_generated/manual)',

    -- Financial
    `funding_source`    VARCHAR(100) NULL                    COMMENT '经费来源(数据字典FUND_SOURCE)',

    -- Department & ownership (for SQL-layer permission)
    `dept_id`           BIGINT       NOT NULL                COMMENT '所属部门ID',
    `created_by`        BIGINT       NOT NULL                COMMENT '创建人ID',
    `created_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`        BIGINT       NULL                    COMMENT '更新人ID',
    `updated_time`      DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),

    -- Link to patent
    INDEX `idx_patent_id` (`patent_id`),

    -- Filter active/paused plans
    INDEX `idx_status` (`status`),

    -- Scan for generation
    INDEX `idx_due_date` (`due_date`),

    -- Data isolation
    INDEX `idx_dept_id` (`dept_id`),

    -- Prevents duplicate plan for same patent/period (D-16, Pitfall 2)
    UNIQUE INDEX `idx_unique_plan` (`patent_id`, `fee_type`, `due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='缴费计划表';
