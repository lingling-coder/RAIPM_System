package com.institute.achievement.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.institute.achievement.module.system.entity.SysMenu;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Menu mapper with custom queries for RBAC permission resolution.
 */
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * Select all menus assigned to a specific role, ordered by sort.
     */
    @Select("SELECT sm.* FROM sys_menu sm JOIN sys_role_menu srm ON sm.id = srm.menu_id " +
            "WHERE srm.role_id = #{roleId} ORDER BY sm.sort_order")
    List<SysMenu> selectMenusByRoleId(Long roleId);

    /**
     * Select all menus assigned to a specific user via their roles, ordered by sort.
     */
    @Select("SELECT DISTINCT sm.* FROM sys_menu sm " +
            "JOIN sys_role_menu srm ON sm.id = srm.menu_id " +
            "JOIN sys_user_role sur ON srm.role_id = sur.role_id " +
            "WHERE sur.user_id = #{userId} ORDER BY sm.sort_order")
    List<SysMenu> selectMenusByUserId(Long userId);

    /**
     * Select all menus sorted by parent_id and sort_order for tree building.
     */
    @Select("SELECT * FROM sys_menu ORDER BY parent_id, sort_order")
    List<SysMenu> selectAllOrderBySort();
}
