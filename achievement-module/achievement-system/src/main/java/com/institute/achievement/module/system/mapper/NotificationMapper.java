package com.institute.achievement.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.institute.achievement.module.system.entity.Notification;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus mapper for Notification entity.
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * Paginated query by user ID and type, ordered by created_time DESC.
     */
    @Select("SELECT * FROM notification WHERE user_id = #{userId} AND type = #{type} ORDER BY created_time DESC")
    IPage<Notification> findByUserIdAndType(IPage<Notification> page, @Param("userId") Long userId, @Param("type") String type);

    /**
     * Count unread notifications for a user.
     */
    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND read_flag = 0")
    Long countUnread(@Param("userId") Long userId);

    /**
     * Mark a single notification as read.
     */
    @Update("UPDATE notification SET read_flag = 1 WHERE id = #{id}")
    void markAsRead(@Param("id") Long id);

    /**
     * Mark all notifications as read for a user.
     */
    @Update("UPDATE notification SET read_flag = 1 WHERE user_id = #{userId} AND read_flag = 0")
    int markAllAsRead(@Param("userId") Long userId);

    /**
     * Delete notifications older than the given cutoff (30-day cleanup).
     * Uses LIMIT for batch delete to avoid long-running locks.
     */
    @Delete("DELETE FROM notification WHERE created_time < #{cutoff} LIMIT 1000")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
