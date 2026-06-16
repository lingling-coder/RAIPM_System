package com.institute.achievement.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.institute.achievement.module.system.dto.MenuTreeNode;
import com.institute.achievement.module.system.entity.SysMenu;
import com.institute.achievement.module.system.entity.SysRoleMenu;
import com.institute.achievement.module.system.mapper.SysMenuMapper;
import com.institute.achievement.module.system.mapper.SysRoleMenuMapper;
import com.institute.achievement.module.system.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Menu service implementation.
 * Builds recursive menu tree for permission assignment UI (P-04).
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public List<MenuTreeNode> buildTree() {
        List<SysMenu> allMenus = menuMapper.selectAllOrderBySort();
        return buildTreeRecursive(allMenus, 0L, null);
    }

    @Override
    public List<MenuTreeNode> getRoleCheckedTree(Long roleId) {
        List<SysMenu> allMenus = menuMapper.selectAllOrderBySort();

        List<Long> checkedMenuIds = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId)
        ).stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());

        return buildTreeRecursive(allMenus, 0L, checkedMenuIds);
    }

    private List<MenuTreeNode> buildTreeRecursive(List<SysMenu> allMenus, Long parentId, List<Long> checkedIds) {
        List<MenuTreeNode> nodes = new ArrayList<>();
        for (SysMenu menu : allMenus) {
            if (menu.getParentId().equals(parentId)) {
                MenuTreeNode node = new MenuTreeNode();
                node.setId(menu.getId());
                node.setParentId(menu.getParentId());
                node.setLabel(menu.getName());
                node.setPermission(menu.getPermission());
                node.setType(menu.getType());
                node.setIcon(menu.getIcon());
                node.setSortOrder(menu.getSortOrder());
                node.setChecked(checkedIds != null && checkedIds.contains(menu.getId()));

                List<MenuTreeNode> children = buildTreeRecursive(allMenus, menu.getId(), checkedIds);
                node.setChildren(children.isEmpty() ? null : children);

                nodes.add(node);
            }
        }
        return nodes;
    }
}
