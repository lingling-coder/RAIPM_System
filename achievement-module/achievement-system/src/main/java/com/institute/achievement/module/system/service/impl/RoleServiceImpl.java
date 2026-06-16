package com.institute.achievement.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.institute.achievement.common.exception.BusinessException;
import com.institute.achievement.common.exception.EntityNotFoundException;
import com.institute.achievement.common.util.PageQuery;
import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.module.system.dto.*;
import com.institute.achievement.module.system.entity.SysMenu;
import com.institute.achievement.module.system.entity.SysRole;
import com.institute.achievement.module.system.entity.SysRoleMenu;
import com.institute.achievement.module.system.entity.SysUserRole;
import com.institute.achievement.module.system.mapper.*;
import com.institute.achievement.module.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Role service implementation.
 * Handles CRUD, user count tracking, and tree-based menu permission assignment (D-18).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements RoleService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public PageResult<RoleVO> page(PageQuery dto) {
        IPage<SysRole> page = this.baseMapper.selectPage(
                new Page<>(dto.getPage(), dto.getPageSize()),
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getDeleted, 0)
        );

        // Build user count per role
        List<Long> roleIds = page.getRecords().stream().map(SysRole::getId).collect(Collectors.toList());
        Map<Long, Integer> userCountMap = getUserCountByRoleIds(roleIds);

        List<RoleVO> voList = page.getRecords().stream().map(role -> {
            RoleVO vo = toRoleVO(role);
            vo.setUserCount(userCountMap.getOrDefault(role.getId(), 0));
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(voList, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    public RoleVO getById(Long id) {
        SysRole role = this.baseMapper.selectById(id);
        if (role == null || role.getDeleted() == 1) {
            throw new EntityNotFoundException("Role", id);
        }
        return toRoleVO(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(RoleCreateDTO dto) {
        // Check role code uniqueness
        Long count = this.baseMapper.selectCount(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, dto.getRoleCode())
        );
        if (count > 0) {
            throw new BusinessException("Role code already exists: " + dto.getRoleCode());
        }

        SysRole role = new SysRole();
        role.setRoleName(dto.getRoleName());
        role.setRoleCode(dto.getRoleCode());
        role.setDescription(dto.getDescription());
        role.setStatus(1);
        this.baseMapper.insert(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(RoleUpdateDTO dto) {
        SysRole role = this.baseMapper.selectById(dto.getId());
        if (role == null || role.getDeleted() == 1) {
            throw new EntityNotFoundException("Role", dto.getId());
        }

        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            role.setStatus(dto.getStatus());
        }
        this.baseMapper.updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = this.baseMapper.selectById(id);
        if (role == null || role.getDeleted() == 1) {
            throw new EntityNotFoundException("Role", id);
        }

        // Check if users are assigned this role
        Long userCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id)
        );
        if (userCount > 0) {
            throw new BusinessException("Cannot delete role '" + role.getRoleName()
                    + "': " + userCount + " user(s) are assigned to this role");
        }

        this.baseMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenuPermissions(Long roleId, List<Long> menuIds) {
        SysRole role = this.baseMapper.selectById(roleId);
        if (role == null || role.getDeleted() == 1) {
            throw new EntityNotFoundException("Role", roleId);
        }

        // Remove existing role-menu associations
        roleMenuMapper.delete(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId)
        );

        // Batch insert new associations
        if (menuIds != null && !menuIds.isEmpty()) {
            List<SysRoleMenu> list = menuIds.stream()
                    .map(menuId -> {
                        SysRoleMenu rm = new SysRoleMenu();
                        rm.setRoleId(roleId);
                        rm.setMenuId(menuId);
                        return rm;
                    })
                    .collect(Collectors.toList());
            roleMenuMapper.insert(list);
        }

        log.info("Menu permissions assigned for role {}: {} menus", roleId,
                menuIds != null ? menuIds.size() : 0);
    }

    @Override
    public List<MenuTreeNode> getMenuTree(Long roleId) {
        // Get all menus assigned to this role
        List<Long> checkedMenuIds = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId)
        ).stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());

        return buildMenuTree(checkedMenuIds);
    }

    @Override
    public List<RoleVO> listAll() {
        List<SysRole> roles = this.baseMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getDeleted, 0)
                        .eq(SysRole::getStatus, 1)
                        .orderByAsc(SysRole::getId)
        );
        return roles.stream().map(this::toRoleVO).collect(Collectors.toList());
    }

    // ── Private Helpers ─────────────────────────────────────────────────────

    private RoleVO toRoleVO(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleCode(role.getRoleCode());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        vo.setCreatedAt(role.getCreatedAt());
        return vo;
    }

    private Map<Long, Integer> getUserCountByRoleIds(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> counts = userRoleMapper.selectMaps(
                new QueryWrapper<SysUserRole>()
                        .in("role_id", roleIds)
                        .groupBy("role_id")
                        .select("role_id, COUNT(*) as count")
        );
        return counts.stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("role_id")).longValue(),
                        m -> ((Number) m.get("count")).intValue()
                ));
    }

    private List<MenuTreeNode> buildMenuTree(List<Long> checkedMenuIds) {
        List<SysMenu> allMenus = menuMapper.selectAllOrderBySort();
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
