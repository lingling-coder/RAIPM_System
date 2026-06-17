-- V18: Add escalation_time column to reminder_task for deadline-relative escalation tracking
-- This enables the escalation scheduler to determine time since last escalation step
-- when deciding whether to advance from DEPT_HEAD to LEADERSHIP (5-day threshold).
ALTER TABLE reminder_task
    ADD COLUMN `escalation_time` DATETIME DEFAULT NULL COMMENT 'Last escalation timestamp (NONE->DEPT_HEAD or DEPT_HEAD->LEADERSHIP)'
    AFTER `escalation_level`;
