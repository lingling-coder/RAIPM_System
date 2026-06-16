package com.institute.achievement.framework.audit;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service interface for the append-only audit log system.
 * <p>
 * Every write operation (login, logout, create, update, delete) is recorded
 * via the {@link #record(AuditLogDTO)} method, forming a SHA-256 hash chain
 * for tamper detection (D-25, D-27).
 */
public interface AuditLogService {

    /**
     * Record a new audit log entry with hash chain linkage.
     * <p>
     * This is the ONLY write method in the service. It:
     * <ol>
     *   <li>Retrieves the previous log entry's current_hash</li>
     *   <li>Computes this entry's current_hash using HashChainUtil</li>
     *   <li>Inserts the entry via the mapper (INSERT-only)</li>
     * </ol>
     *
     * @param dto the audit log data to record
     */
    void record(AuditLogDTO dto);

    /**
     * Paginated query with filters.
     *
     * @param dto page query with filter criteria
     * @return paginated result of audit log view objects
     */
    com.institute.achievement.common.util.PageResult<AuditLogVO> page(AuditLogPageDTO dto);

    /**
     * Get a single audit log entry detail with hash chain integrity verification.
     *
     * @param id the audit log entry ID
     * @return audit log VO with integrityVerified field populated
     */
    AuditLogVO getDetail(Long id);

    /**
     * Verify hash chain integrity for a range of entries.
     *
     * @param fromId start of the range (inclusive)
     * @param toId   end of the range (inclusive)
     * @return verification result with validity flag and any broken links
     */
    ChainVerificationResult verifyChain(Long fromId, Long toId);

    /**
     * Get operation type statistics for dashboard display.
     *
     * @param start start time (inclusive)
     * @param end   end time (inclusive)
     * @return map of operation type to count
     */
    Map<String, Long> getOperationTypeStats(LocalDateTime start, LocalDateTime end);
}
