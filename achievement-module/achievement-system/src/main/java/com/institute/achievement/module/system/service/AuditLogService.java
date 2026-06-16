package com.institute.achievement.module.system.service;

import com.institute.achievement.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Simple audit log service for Phase 1.
 * <p>
 * Inserts records into the existing audit_log table with a simplified
 * set of fields. Phase 0 will replace this with the full hash-chain
 * audit log service, but the table structure already supports it.
 * <p>
 * This minimal implementation avoids introducing a MyBatis-Plus entity
 * for audit_log (which belongs to Phase 0's framework module).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Log an audit trail entry.
     *
     * @param operation  operation name (e.g., "SUBMIT", "APPROVE", "REJECT")
     * @param entityId   the entity ID the operation was performed on
     * @param userId     the user who performed the operation
     * @param detail     human-readable detail (e.g., "状态: DRAFT -> PENDING_DEPT_REVIEW")
     */
    @Transactional
    public void log(String operation, Long entityId, Long userId, String detail) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            String ipAddress = SecurityUtils.getClientIp();

            jdbcTemplate.update(
                "INSERT INTO audit_log (operator_id, operator_name, operation_type, operation_name, " +
                "target_type, target_id, target_content, ip_address, created_at, previous_hash, current_hash, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), '', ?, 1)",
                userId,
                username,
                operation,
                detail,
                "ACHIEVEMENT",
                String.valueOf(entityId),
                detail,
                ipAddress,
                java.util.UUID.randomUUID().toString().replace("-", "")
            );
        } catch (Exception e) {
            log.warn("Failed to write audit log (non-fatal): {}", e.getMessage());
        }
    }
}
