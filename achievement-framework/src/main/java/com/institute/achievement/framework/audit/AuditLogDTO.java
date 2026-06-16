package com.institute.achievement.framework.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for recording a new audit log entry.
 * Passed from the AOP interceptor (or manual calls) to AuditLogService.record().
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {

    /** User ID of the operator */
    private Long operatorId;

    /** Username of the operator */
    private String operatorName;

    /** LOGIN / LOGOUT / CREATE / UPDATE / DELETE */
    private String operationType;

    /** Human-readable operation description */
    private String operationName;

    /** Target object type, e.g. "User" / "Role" */
    private String targetType;

    /** Target object ID */
    private String targetId;

    /** Content before the operation (will be JSON-serialized) */
    private Object originalContent;

    /** Content after the operation (will be JSON-serialized) */
    private Object targetContent;

    /** Client IP address */
    private String ipAddress;

    /** Client User-Agent string */
    private String userAgent;

    /** 1 = success, 0 = failure */
    private Integer status;
}
