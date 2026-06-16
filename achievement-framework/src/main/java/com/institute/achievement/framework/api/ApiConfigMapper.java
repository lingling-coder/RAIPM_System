package com.institute.achievement.framework.api;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis-Plus mapper for the api_config table.
 * <p>
 * Provides CRUD operations via BaseMapper plus custom queries
 * for runtime config loading by status and code.
 */
@Mapper
public interface ApiConfigMapper extends BaseMapper<ApiConfigEntity> {

    /**
     * Select all configs with the given status.
     *
     * @param status 1=enabled, 0=disabled
     * @return list of matching configurations
     */
    @Select("SELECT * FROM api_config WHERE status = #{status}")
    List<ApiConfigEntity> selectByStatus(Integer status);

    /**
     * Select a single config by its unique code.
     *
     * @param code the configuration code
     * @return the matching configuration, or null if not found
     */
    @Select("SELECT * FROM api_config WHERE config_code = #{code}")
    ApiConfigEntity selectByCode(String code);
}
