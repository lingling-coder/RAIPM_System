package com.institute.achievement.module.system.controller;

import com.institute.achievement.common.util.PageQuery;
import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.common.util.Result;
import com.institute.achievement.module.system.dto.DictEntryDTO;
import com.institute.achievement.module.system.service.DictEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Dictionary entry REST controller.
 * Manages right-side entry table in P-06.
 */
@RestController
@RequestMapping("/api/system/dict-entry")
@RequiredArgsConstructor
public class DictEntryController {

    private final DictEntryService dictEntryService;

    /**
     * Paginated entries filtered by category and keyword.
     */
    @PostMapping("/page")
    public Result<PageResult<DictEntryDTO>> page(@RequestBody DictEntryPageQuery dto) {
        return Result.success(dictEntryService.page(dto.getCategoryId(), dto.getKeyword(), dto));
    }

    /**
     * Create new entry.
     */
    @PostMapping
    public Result<Void> create(@RequestBody DictEntryDTO dto) {
        dictEntryService.create(dto);
        return Result.success();
    }

    /**
     * Update entry.
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody DictEntryDTO dto) {
        dto.setId(id);
        dictEntryService.update(dto);
        return Result.success();
    }

    /**
     * Delete entry.
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dictEntryService.delete(id);
        return Result.success();
    }

    /**
     * Internal DTO extending PageQuery with dict entry filter fields.
     */
    @lombok.Data
    @lombok.EqualsAndHashCode(callSuper = true)
    public static class DictEntryPageQuery extends PageQuery {
        private Long categoryId;
        private String keyword;
    }
}
