package com.institute.achievement.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.institute.achievement.module.system.entity.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis-Plus mapper for ApprovalRecord entity.
 */
@Mapper
public interface ApprovalRecordMapper extends BaseMapper<ApprovalRecord> {

    /**
     * Find approval records for a specific achievement, ordered by time ASC.
     */
    @Select("SELECT * FROM approval_record WHERE achievement_type = #{type} AND achievement_id = #{id} ORDER BY created_time ASC")
    List<ApprovalRecord> findByAchievement(@Param("type") String type, @Param("id") Long id);
}
