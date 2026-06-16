package com.institute.achievement.module.system.controller;

import com.institute.achievement.common.util.Result;
import com.institute.achievement.module.system.dto.DictCategoryDTO;
import com.institute.achievement.module.system.service.DictCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Dictionary category REST controller.
 * Manages left-side tree categories in P-06.
 */
@RestController
@RequestMapping("/api/system/dict-category")
@RequiredArgsConstructor
public class DictCategoryController {

    private final DictCategoryService dictCategoryService;

    /**
     * List all categories ordered by sort.
     */
    @GetMapping("/list")
    public Result<List<DictCategoryDTO>> listAll() {
        return Result.success(dictCategoryService.listAll());
    }

    /**
     * Create new category.
     */
    @PostMapping
    public Result<Void> create(@RequestBody DictCategoryDTO dto) {
        dictCategoryService.create(dto);
        return Result.success();
    }

    /**
     * Update category.
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody DictCategoryDTO dto) {
        dto.setId(id);
        dictCategoryService.update(dto);
        return Result.success();
    }

    /**
     * Delete category. Checks if entries exist first.
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dictCategoryService.delete(id);
        return Result.success();
    }
}
