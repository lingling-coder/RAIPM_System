package com.institute.achievement.module.system.service;

import com.institute.achievement.module.system.dto.MenuTreeNode;

import java.util.List;

/**
 * Menu service interface.
 * Builds menu tree for permission assignment UI.
 */
public interface MenuService {

    /**
     * Build full menu tree from flat menu data.
     */
    List<MenuTreeNode> buildTree();

    /**
     * Build menu tree with checked flags for a specific role.
     */
    List<MenuTreeNode> getRoleCheckedTree(Long roleId);
}
