package com.institute.achievement.reminder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.institute.achievement.reminder.entity.ReminderTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis-Plus mapper for ReminderTask entity.
 * <p>
 * Provides standard CRUD via BaseMapper plus queries for escalation
 * scanning and high-urgency task resolution.
 */
@Mapper
public interface ReminderTaskMapper extends BaseMapper<ReminderTask> {

    /**
     * Find unconfirmed tasks for escalation scanning.
     * <p>
     * Returns all unconfirmed tasks where the deadline has not yet passed,
     * ordered by deadline ascending (most urgent first).
     *
     * @return list of unconfirmed tasks ready for escalation evaluation
     */
    @Select("SELECT * FROM reminder_task WHERE confirmed_flag = 0 AND deadline >= CURRENT_DATE ORDER BY deadline ASC")
    List<ReminderTask> findUnconfirmedForEscalation();

    /**
     * Find unconfirmed HIGH urgency tasks for a specific user.
     * <p>
     * Used by the high-urgency global popup feature to fetch tasks
     * that need user attention.
     *
     * @param userId the user ID to query
     * @return list of unconfirmed HIGH urgency tasks
     */
    @Select("SELECT * FROM reminder_task WHERE user_id = #{userId} AND confirmed_flag = 0 AND urgency = 'HIGH' AND deadline >= CURRENT_DATE ORDER BY deadline ASC")
    List<ReminderTask> findUnconfirmedHighUrgency(@Param("userId") Long userId);

    /**
     * Count tasks referencing a specific config (used by config delete guard).
     *
     * @param configId the config ID
     * @return count of tasks referencing this config
     */
    @Select("SELECT COUNT(*) FROM reminder_task WHERE config_id = #{configId}")
    int countByConfigId(@Param("configId") Long configId);
}
