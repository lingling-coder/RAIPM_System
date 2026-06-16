package com.institute.achievement.module.system.service;

import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.common.util.PageQuery;
import com.institute.achievement.module.system.dto.*;

import java.util.List;

/**
 * Department management service interface.
 * Flat structure per D-08.
 */
public interface DepartmentService {

    /**
     * Paginated department list with member count.
     */
    PageResult<DepartmentVO> page(PageQuery dto);

    /**
     * Get department by ID.
     */
    DepartmentVO getById(Long id);

    /**
     * Create new department.
     */
    void create(DepartmentCreateDTO dto);

    /**
     * Update department.
     */
    void update(DepartmentUpdateDTO dto);

    /**
     * Delete department. Blocks if members exist (throws BusinessException).
     */
    void delete(Long id);

    /**
     * List all departments for dropdown.
     */
    List<DepartmentVO> listAll();
}
