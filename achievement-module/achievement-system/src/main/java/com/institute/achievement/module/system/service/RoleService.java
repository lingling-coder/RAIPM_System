package com.institute.achievement.module.system.service;

import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.common.util.PageQuery;
import com.institute.achievement.module.system.dto.*;

import java.util.List;

/**
 * Role management service interface.
 * Handles CRUD, user count tracking, and menu permission assignment.
 */
public interface RoleService {

    /**
     * Paginated role list with user count.
     */
    PageResult<RoleVO> page(PageQuery dto);

    /**
     * Get role by ID.
     */
    RoleVO getById(Long id);

    /**
     * Create new role.
     */
    void create(RoleCreateDTO dto);

    /**
     * Update role info.
     */
    void update(RoleUpdateDTO dto);

    /**
     * Delete role. Blocks if users are assigned (throws BusinessException).
     */
    void delete(Long id);

    /**
     * Assign menu permissions to role (D-18).
     * Removes existing role_menu entries and batch inserts new ones.
     */
    void assignMenuPermissions(Long roleId, List<Long> menuIds);

    /**
     * Get full menu tree with checked state for role permission assignment.
     */
    List<MenuTreeNode> getMenuTree(Long roleId);

    /**
     * List all roles for dropdown selection.
     */
    List<RoleVO> listAll();
}
