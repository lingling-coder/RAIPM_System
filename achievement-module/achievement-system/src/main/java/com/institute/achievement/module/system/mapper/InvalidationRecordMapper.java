package com.institute.achievement.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.institute.achievement.module.system.entity.InvalidationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis-Plus mapper for InvalidationRecord entity.
 * <p>
 * Provides standard CRUD operations plus custom queries for
 * retrieving invalidation records by achievement.
 */
@Mapper
public interface InvalidationRecordMapper extends BaseMapper<InvalidationRecord> {

    /**
     * Find invalidation records for a specific achievement, ordered by time DESC.
     */
    @Select("SELECT * FROM invalidation_record WHERE achievement_type = #{type} AND achievement_id = #{id} ORDER BY created_time DESC")
    List<InvalidationRecord> findByAchievement(@Param("type") String type, @Param("id") Long id);

    /**
     * Find all invalidation records by invalidator, ordered by time DESC.
     */
    @Select("SELECT * FROM invalidation_record WHERE invalidator_id = #{invalidatorId} ORDER BY created_time DESC")
    List<InvalidationRecord> findByInvalidator(@Param("invalidatorId") Long invalidatorId);
}
