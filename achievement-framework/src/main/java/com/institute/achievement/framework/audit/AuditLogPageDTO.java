package com.institute.achievement.framework.audit;

import com.institute.achievement.common.util.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Page query DTO for filtering audit log entries.
 * Extends PageQuery for pagination support.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogPageDTO extends PageQuery {

    /** Filter by operator name (fuzzy) */
    private String operatorName;

    /** Filter by operation type (LOGIN/LOGOUT/CREATE/UPDATE/DELETE) */
    private String operationType;

    /** Filter by target type */
    private String targetType;

    /** Filter start time (inclusive) */
    private LocalDateTime startTime;

    /** Filter end time (inclusive) */
    private LocalDateTime endTime;
}
