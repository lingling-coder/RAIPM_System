package com.institute.achievement.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.institute.achievement.module.system.entity.SysRole;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Role mapper with custom query for user-role association.
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * Select all roles assigned to a specific user.
     */
    @Select("SELECT sr.* FROM sys_role sr JOIN sys_user_role sur ON sr.id = sur.role_id WHERE sur.user_id = #{userId}")
    List<SysRole> selectRolesByUserId(Long userId);
}
