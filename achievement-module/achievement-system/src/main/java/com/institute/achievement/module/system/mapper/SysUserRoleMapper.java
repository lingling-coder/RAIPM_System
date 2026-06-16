package com.institute.achievement.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.institute.achievement.module.system.entity.SysUserRole;

/**
 * User-Role association mapper.
 * Required for multi-role support per D-17.
 */
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
