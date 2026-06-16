package com.institute.achievement.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.institute.achievement.module.system.entity.SysUser;
import org.apache.ibatis.annotations.Select;

/**
 * User mapper. BaseMapper provides CRUD + logic delete operations.
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * Find user by username (used during authentication).
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    SysUser selectByUsername(String username);
}
