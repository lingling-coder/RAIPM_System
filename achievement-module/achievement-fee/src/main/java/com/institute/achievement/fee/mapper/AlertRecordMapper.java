package com.institute.achievement.fee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.fee.dto.AlertQueryDTO;
import com.institute.achievement.fee.dto.AlertRecordVO;
import com.institute.achievement.fee.entity.AlertRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MyBatis-Plus mapper for AlertRecord entity.
 * <p>
 * Provides standard CRUD via BaseMapper plus custom paginated listing
 * with JOINs to fee_record, patent, and copyright tables for owner name
 * resolution, plus dedup and escalation queries.
 */
@Mapper
public interface AlertRecordMapper extends BaseMapper<AlertRecord> {

    /**
     * Paginated alert record listing with JOINs.
     * <p>
     * JOINs with fee_record for financial details and with patent/copyright
     * tables for owner name. Uses SQL-layer data isolation via dept_id
     * injection (MyBatis-Plus interceptor).
     *
     * @param page  MyBatis-Plus pagination object
     * @param query filter parameters (status, alertLevel, deptId)
     * @return paginated alert record VO list
     */
    @Select("<script>"
            + "SELECT ar.id, ar.fee_record_id, ar.alert_level, ar.triggered_date, "
            + "  ar.triggered_at, ar.resolved_at, ar.status, ar.escalation_level, "
            + "  fr.amount AS fee_amount, fr.due_date, fr.owner_type, fr.owner_id, "
            + "  CASE "
            + "    WHEN fr.owner_type = 'patent' THEN p.patent_name "
            + "    WHEN fr.owner_type = 'copyright' THEN c.name "
            + "    ELSE NULL "
            + "  END AS owner_name "
            + "FROM alert_record ar "
            + "INNER JOIN fee_record fr ON ar.fee_record_id = fr.id "
            + "LEFT JOIN patent p ON fr.owner_type = 'patent' AND fr.owner_id = p.id "
            + "LEFT JOIN software_copyright c ON fr.owner_type = 'copyright' AND fr.owner_id = c.id "
            + "WHERE 1=1 "
            + "<if test='query.status != null and query.status != \"\"'>"
            + "  AND ar.status = #{query.status} "
            + "</if>"
            + "<if test='query.alertLevel != null and query.alertLevel != \"\"'>"
            + "  AND ar.alert_level = #{query.alertLevel} "
            + "</if>"
            + "<if test='query.deptId != null'>"
            + "  AND ar.dept_id = #{query.deptId} "
            + "</if>"
            + "ORDER BY ar.triggered_at DESC "
            + "</script>")
    Page<AlertRecordVO> selectAlertPage(Page<?> page, @Param("query") AlertQueryDTO query);

    /**
     * Find alert records by status and level.
     *
     * @param status the status filter (e.g. "pending")
     * @param level  the alert level filter (e.g. "RED")
     * @return list of matching alert records
     */
    @Select("SELECT * FROM alert_record WHERE status = #{status} AND alert_level = #{level} ORDER BY triggered_at DESC")
    List<AlertRecord> findByStatusAndLevel(@Param("status") String status, @Param("level") String level);

    /**
     * Find pending alerts that haven't been resolved past the cutoff time,
     * used for escalation scan (Phase 2).
     *
     * @param cutoffTime the cutoff time for pending alerts
     * @return list of pending alert records triggered at or before cutoffTime
     */
    @Select("SELECT * FROM alert_record WHERE status = 'pending' AND triggered_at &lt;= #{cutoffTime} ORDER BY triggered_at ASC")
    List<AlertRecord> findPendingForEscalation(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count alert records for a specific fee_record + level + date combination.
     * Used for dedup check before inserting a new alert.
     *
     * @param feeRecordId  the fee record ID
     * @param alertLevel   the alert level code
     * @param triggeredDate the trigger date
     * @return count of matching records (0 = no duplicate)
     */
    @Select("SELECT COUNT(*) FROM alert_record "
            + "WHERE fee_record_id = #{feeRecordId} "
            + "AND alert_level = #{alertLevel} "
            + "AND triggered_date = #{triggeredDate}")
    int countByFeeRecordAndLevelAndDate(@Param("feeRecordId") Long feeRecordId,
                                        @Param("alertLevel") String alertLevel,
                                        @Param("triggeredDate") LocalDate triggeredDate);

    /**
     * Batch dedup: returns all fee_record_ids that already have alerts for today.
     * <p>
     * Used before the daily scan to pre-filter which fee_records need new alerts.
     *
     * @param date the trigger date to check
     * @return list of fee_record_ids that already have alerts for this date
     */
    @Select("SELECT DISTINCT fee_record_id FROM alert_record WHERE triggered_date = #{date}")
    List<Long> findAlreadyAlertedFeeRecordIds(@Param("triggeredDate") LocalDate date);
}
