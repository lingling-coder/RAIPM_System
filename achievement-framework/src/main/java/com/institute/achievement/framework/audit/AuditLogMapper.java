package com.institute.achievement.framework.audit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MyBatis-Plus mapper for the audit_log table.
 * <p>
 * CRITICAL: This mapper is INSERT-only. No updateById or deleteById methods
 * are exposed. The only write operation available is insert() from BaseMapper.
 * The application layer enforces append-only semantics (D-25, D-27).
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {

    /**
     * Paginated query with dynamic filters.
     * Supports filtering by operator name, operation type, target type, and time range.
     */
    @Select({
            "<script>",
            "SELECT * FROM audit_log",
            "WHERE 1=1",
            "<if test='operatorName != null and operatorName != \"\"'>",
            "  AND operator_name LIKE CONCAT('%', #{operatorName}, '%')",
            "</if>",
            "<if test='operationType != null and operationType != \"\"'>",
            "  AND operation_type = #{operationType}",
            "</if>",
            "<if test='targetType != null and targetType != \"\"'>",
            "  AND target_type LIKE CONCAT('%', #{targetType}, '%')",
            "</if>",
            "<if test='startTime != null'>",
            "  AND created_at &gt;= #{startTime}",
            "</if>",
            "<if test='endTime != null'>",
            "  AND created_at &lt;= #{endTime}",
            "</if>",
            "ORDER BY id DESC",
            "</script>"
    })
    Page<AuditLogEntity> selectPageWithFilters(
            Page<AuditLogEntity> page,
            @Param("operatorName") String operatorName,
            @Param("operationType") String operationType,
            @Param("targetType") String targetType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Get the last (most recent) audit log entry.
     * Used to retrieve the current_hash for computing the next entry's previous_hash.
     */
    @Select("SELECT * FROM audit_log ORDER BY id DESC LIMIT 1")
    AuditLogEntity selectLastLog();

    /**
     * Query a contiguous segment of the audit log by ID range.
     * Used for hash chain integrity verification.
     */
    @Select("SELECT * FROM audit_log WHERE id BETWEEN #{fromId} AND #{toId} ORDER BY id ASC")
    List<AuditLogEntity> selectChainSegment(@Param("fromId") Long fromId, @Param("toId") Long toId);

    /**
     * Count audit log entries by operation type within a time range (for dashboard stats).
     */
    @Select({
            "<script>",
            "SELECT operation_type, COUNT(*) as cnt",
            "FROM audit_log",
            "WHERE 1=1",
            "<if test='startTime != null'>",
            "  AND created_at &gt;= #{startTime}",
            "</if>",
            "<if test='endTime != null'>",
            "  AND created_at &lt;= #{endTime}",
            "</if>",
            "GROUP BY operation_type",
            "</script>"
    })
    List<OperationTypeCount> countByOperationType(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Internal DTO for operation type count aggregation.
     */
    interface OperationTypeCount {
        String getOperationType();

        long getCnt();
    }
}
