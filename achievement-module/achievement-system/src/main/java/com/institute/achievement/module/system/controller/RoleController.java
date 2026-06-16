package com.institute.achievement.module.system.controller;

import com.institute.achievement.common.util.PageQuery;
import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.common.util.Result;
import com.institute.achievement.module.system.dto.*;
import com.institute.achievement.module.system.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Role management REST controller.
 * Provides CRUD operations and tree-based menu permission assignment (D-18).
 */
@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * Paginated role list with user count.
     */
    @PostMapping("/page")
    public Result<PageResult<RoleVO>> page(@RequestBody PageQuery dto) {
        return Result.success(roleService.page(dto));
    }

    /**
     * Get role by ID.
     */
    @GetMapping("/{id}")
    public Result<RoleVO> getById(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }

    /**
     * Create new role.
     */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody RoleCreateDTO dto) {
        roleService.create(dto);
        return Result.success();
    }

    /**
     * Update role.
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        dto.setId(id);
        roleService.update(dto);
        return Result.success();
    }

    /**
     * Delete role. Blocks if users are assigned.
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }

    /**
     * Get menu tree with checked state for role permission assignment (D-18).
     */
    @GetMapping("/{id}/menu-tree")
    public Result<List<MenuTreeNode>> getMenuTree(@PathVariable Long id) {
        return Result.success(roleService.getMenuTree(id));
    }

    /**
     * Assign menu permissions to role (D-18).
     */
    @PutMapping("/{id}/menu-permissions")
    public Result<Void> assignMenuPermissions(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenuPermissions(id, menuIds);
        return Result.success();
    }

    /**
     * List all roles for dropdown selection.
     */
    @GetMapping("/list-all")
    public Result<List<RoleVO>> listAll() {
        return Result.success(roleService.listAll());
    }
}
