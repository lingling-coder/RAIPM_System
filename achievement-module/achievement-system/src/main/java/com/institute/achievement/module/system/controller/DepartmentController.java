package com.institute.achievement.module.system.controller;

import com.institute.achievement.common.util.PageQuery;
import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.common.util.Result;
import com.institute.achievement.module.system.dto.*;
import com.institute.achievement.module.system.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Department management REST controller.
 * Flat structure per D-08.
 */
@RestController
@RequestMapping("/api/system/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * Paginated department list with member count.
     */
    @PostMapping("/page")
    public Result<PageResult<DepartmentVO>> page(@RequestBody PageQuery dto) {
        return Result.success(departmentService.page(dto));
    }

    /**
     * Get department by ID.
     */
    @GetMapping("/{id}")
    public Result<DepartmentVO> getById(@PathVariable Long id) {
        return Result.success(departmentService.getById(id));
    }

    /**
     * Create new department.
     */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody DepartmentCreateDTO dto) {
        departmentService.create(dto);
        return Result.success();
    }

    /**
     * Update department.
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DepartmentUpdateDTO dto) {
        dto.setId(id);
        departmentService.update(dto);
        return Result.success();
    }

    /**
     * Delete department. Blocks if members exist.
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return Result.success();
    }

    /**
     * List all departments for dropdown.
     */
    @GetMapping("/list-all")
    public Result<List<DepartmentVO>> listAll() {
        return Result.success(departmentService.listAll());
    }
}
