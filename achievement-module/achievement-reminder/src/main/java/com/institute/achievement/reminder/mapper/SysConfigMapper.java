package com.institute.achievement.reminder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.institute.achievement.reminder.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * MyBatis-Plus mapper for the sys_config table.
 * <p>
 * Provides key-value style configuration lookups used by EmailServiceImpl
 * to read SMTP settings at runtime (D-14).
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    /**
     * Get a configuration value by its key.
     *
     * @param key the config key (e.g., "smtp.host")
     * @return the config value as string, or null if not found
     */
    @Select("SELECT config_value FROM sys_config WHERE config_key = #{key}")
    String getValueByKey(@Param("key") String key);

    /**
     * Get a full config entry by its key.
     *
     * @param key the config key (e.g., "smtp.host")
     * @return the full SysConfig entity, or null if not found
     */
    @Select("SELECT * FROM sys_config WHERE config_key = #{key}")
    SysConfig getByKey(@Param("key") String key);
}
