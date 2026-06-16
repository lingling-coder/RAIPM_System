package com.institute.achievement.module.system.service;

import com.institute.achievement.common.util.PageQuery;
import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.module.system.dto.DictEntryDTO;

/**
 * Dictionary entry service interface.
 * Manages right-side entry table in P-06.
 */
public interface DictEntryService {

    /**
     * Paginated entries filtered by category and keyword.
     */
    PageResult<DictEntryDTO> page(Long categoryId, String keyword, PageQuery dto);

    /**
     * Create new entry. Checks unique key within category.
     */
    void create(DictEntryDTO dto);

    /**
     * Update entry.
     */
    void update(DictEntryDTO dto);

    /**
     * Delete entry.
     */
    void delete(Long id);
}
