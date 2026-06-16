-- V11: Create fee_record table
-- Core table for patent/software copyright fee management (FEE-01).
-- Uses Arc polymorphic pattern (owner_type + owner_id) to associate with
-- either patent or copyright achievements (D-10).
-- Supports the full fee lifecycle: pending → paid | paused.

CREATE TABLE `fee_record` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    -- Fee type (D-12)
    `fee_type`             VARCHAR(50)  NOT NULL                COMMENT '费用类型(annual_fee/registration_fee/maintenance_fee/other)',

    -- Amounts
    `amount`               DECIMAL(12,2) NOT NULL               COMMENT '金额(元)',
    `paid_amount`          DECIMAL(12,2) NULL                   COMMENT '实缴金额(元)',

    -- Dates
    `due_date`             DATE         NOT NULL                COMMENT '缴费截止日期',
    `paid_date`            DATE         NULL                    COMMENT '实际缴费日期',

    -- Payment evidence
    `voucher_no`           VARCHAR(100) NULL                    COMMENT '缴费凭证号',

    -- Status
    `status`               VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '缴费状态(pending/paid/paused)',

    -- Financial
    `funding_source`       VARCHAR(100) NULL                    COMMENT '经费来源(数据字典FUND_SOURCE)',

    -- Polymorphic Arc (D-10)
    `owner_type`           VARCHAR(50)  NOT NULL                COMMENT '关联成果类型(patent/copyright)',
    `owner_id`             BIGINT       NOT NULL                COMMENT '关联成果ID',

    -- Source
    `source`               VARCHAR(20)  NOT NULL DEFAULT 'manual' COMMENT '记录来源(auto_generated/manual)',

    -- Batch slip (02-04)
    `slip_no`              VARCHAR(100) NULL                    COMMENT '缴费单编号(FEE-YYYYMMDD-XXX)',
    `slip_generated_time`  DATETIME     NULL                    COMMENT '缴费单生成时间',
    `slip_generated_by`    BIGINT       NULL                    COMMENT '缴费单生成人ID',

    -- Department & ownership (for SQL-layer permission)
    `dept_id`              BIGINT       NOT NULL                COMMENT '所属部门ID',
    `created_by`           BIGINT       NOT NULL                COMMENT '创建人ID',
    `created_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by`           BIGINT       NULL                    COMMENT '更新人ID',
    `updated_time`         DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),

    -- Arc polymorphic query support
    INDEX `idx_owner` (`owner_type`, `owner_id`),

    -- Filter and query performance indexes
    INDEX `idx_status` (`status`),
    INDEX `idx_due_date` (`due_date`),
    INDEX `idx_dept_id` (`dept_id`),
    INDEX `idx_funding_source` (`funding_source`),

    -- Prevents duplicate fee records for the same achievement, type, and due date (D-13)
    UNIQUE INDEX `idx_unique_fee` (`owner_type`, `owner_id`, `fee_type`, `due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='费用记录表';
