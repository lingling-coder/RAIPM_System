package com.institute.achievement.framework.audit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Audit log entity mapped to the audit_log table.
 * <p>
 * This entity represents a single audit trail record. The table uses MySQL
 * RANGE partitioning by month (D-26). Records are INSERT-only -- no update
 * or delete operations are exposed on the mapper (D-25).
 * <p>
 * Each entry participates in a SHA-256 hash chain (D-27): {@code previous_hash}
 * stores the hash of the preceding entry, and {@code current_hash} is computed
 * from this entry's content plus the previous hash.
 */
@Data
@TableName("audit_log")
public class AuditLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** User ID of the operator */
    private Long operatorId;

    /** Username of the operator */
    private String operatorName;

    /** LOGIN / LOGOUT / CREATE / UPDATE / DELETE */
    private String operationType;

    /** Human-readable operation description, e.g. "登录系统" */
    private String operationName;

    /** Target object type, e.g. "User" / "Role" / "Department" */
    private String targetType;

    /** Target object ID */
    private String targetId;

    /** JSON content after the operation */
    private String targetContent;

    /** JSON content before the operation (null for CREATE) */
    private String originalContent;

    /** Client IP address */
    private String ipAddress;

    /** Client User-Agent string */
    private String userAgent;

    /** 1 = success, 0 = failure */
    private Integer status;

    /** SHA-256 hash of the previous audit log entry */
    private String previousHash;

    /** SHA-256 hash of this entry */
    private String currentHash;

    /** Record creation timestamp */
    private LocalDateTime createdAt;
}
