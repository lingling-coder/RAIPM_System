package com.institute.achievement.framework.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * View object for audit log entries returned to the frontend.
 * Includes hash chain integrity verification status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogVO {

    private Long id;

    /** Username of the operator */
    private String operatorName;

    /** LOGIN / LOGOUT / CREATE / UPDATE / DELETE */
    private String operationType;

    /** Human-readable operation description */
    private String operationName;

    /** Target object type */
    private String targetType;

    /** Target object ID */
    private String targetId;

    /** Client IP address */
    private String ipAddress;

    /** Client User-Agent */
    private String userAgent;

    /** 1 = success, 0 = failure */
    private Integer status;

    /** JSON content before the operation */
    private String originalContent;

    /** JSON content after the operation */
    private String targetContent;

    /** SHA-256 hash of the previous entry */
    private String previousHash;

    /** SHA-256 hash of this entry */
    private String currentHash;

    /** Whether the hash chain integrity is verified for this entry */
    private Boolean integrityVerified;

    /** Record creation timestamp */
    private LocalDateTime createdAt;
}
