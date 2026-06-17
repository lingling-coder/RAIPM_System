package com.institute.achievement.reminder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.reminder.entity.ReminderConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis-Plus mapper for ReminderConfig entity.
 * <p>
 * Provides standard CRUD via BaseMapper plus custom paginated listing
 * with dynamic filtering by type code, and query for enabled configs
 * used by the daily scheduler.
 */
@Mapper
public interface ReminderConfigMapper extends BaseMapper<ReminderConfig> {

    /**
     * Paginated config listing with dynamic filter.
     *
     * @param page     MyBatis-Plus pagination object
     * @param typeCode optional type code filter
     * @return paginated config list
     */
    @Select("<script>"
            + "SELECT * FROM reminder_config "
            + "WHERE 1=1 "
            + "<if test='typeCode != null and typeCode != \"\"'>"
            + "  AND type_code = #{typeCode} "
            + "</if>"
            + "ORDER BY created_time DESC "
            + "</script>")
    Page<ReminderConfig> selectConfigPage(Page<?> page, @Param("typeCode") String typeCode);

    /**
     * Find all enabled configs for the daily task scheduler.
     *
     * @return list of enabled reminder configs
     */
    @Select("SELECT * FROM reminder_config WHERE status = 1")
    List<ReminderConfig> findEnabledConfigs();
}
