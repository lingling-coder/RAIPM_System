package com.institute.achievement.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.institute.achievement.module.system.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * User mapper. BaseMapper provides CRUD + logic delete operations.
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * Find user by username (used during authentication).
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    SysUser selectByUsername(String username);

    /**
     * Find all user IDs in a department that have a specific role.
     * <p>
     * Uses RBAC JOIN query: sys_user -> sys_user_role -> sys_role.
     * Used by the alert escalation engine to route escalation notifications
     * to the correct department heads / leaders.
     *
     * @param deptId   the department ID
     * @param roleCode the role code (e.g. "ROLE_SECRETARY", "ROLE_LEADER")
     * @return list of matching user IDs (may be empty)
     */
    @Select("SELECT DISTINCT u.id FROM sys_user u "
            + "JOIN sys_user_role ur ON u.id = ur.user_id "
            + "JOIN sys_role r ON ur.role_id = r.id "
            + "WHERE u.dept_id = #{deptId} AND r.role_code = #{roleCode} AND u.deleted = 0")
    List<Long> findUserIdsByDeptAndRole(@Param("deptId") Long deptId, @Param("roleCode") String roleCode);
}
