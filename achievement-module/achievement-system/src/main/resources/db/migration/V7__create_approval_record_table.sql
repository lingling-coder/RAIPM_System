CREATE TABLE `approval_record` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    achievement_type VARCHAR(50) NOT NULL COMMENT 'paper/patent/copyright',
    achievement_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL COMMENT 'SUBMIT/PASS_DEPT/PASS_ADMIN/REJECT_DEPT/REJECT_ADMIN/WITHDRAW/RESUBMIT',
    operator_id BIGINT NOT NULL COMMENT '操作人用户ID',
    operator_name VARCHAR(100) NOT NULL COMMENT '操作人姓名',
    comment TEXT NULL COMMENT '审批意见/退回原因',
    from_status VARCHAR(50) NOT NULL COMMENT '操作前状态',
    to_status VARCHAR(50) NOT NULL COMMENT '操作后状态',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_achievement (achievement_type, achievement_id),
    INDEX idx_operator (operator_id),
    INDEX idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
