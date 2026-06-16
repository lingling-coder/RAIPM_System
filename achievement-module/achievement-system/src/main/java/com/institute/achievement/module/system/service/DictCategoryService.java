package com.institute.achievement.module.system.service;

import com.institute.achievement.module.system.dto.DictCategoryDTO;

import java.util.List;

/**
 * Dictionary category service interface.
 * Manages left-side tree categories in P-06.
 */
public interface DictCategoryService {

    /**
     * List all categories ordered by sort.
     */
    List<DictCategoryDTO> listAll();

    /**
     * Create new category.
     */
    void create(DictCategoryDTO dto);

    /**
     * Update category.
     */
    void update(DictCategoryDTO dto);

    /**
     * Delete category. Checks if entries exist first.
     */
    void delete(Long id);
}
