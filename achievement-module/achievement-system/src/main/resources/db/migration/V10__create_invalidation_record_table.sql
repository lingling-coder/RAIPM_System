CREATE TABLE `invalidation_record` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    achievement_type VARCHAR(50) NOT NULL COMMENT 'paper/patent/copyright',
    achievement_id BIGINT NOT NULL,
    invalidator_id BIGINT NOT NULL,
    invalidator_name VARCHAR(100) NOT NULL,
    reason TEXT NOT NULL COMMENT '作废原因',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_achievement (achievement_type, achievement_id),
    INDEX idx_invalidator (invalidator_id),
    INDEX idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
