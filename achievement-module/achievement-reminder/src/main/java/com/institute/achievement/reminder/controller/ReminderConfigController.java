package com.institute.achievement.reminder.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.util.Result;
import com.institute.achievement.reminder.dto.ReminderConfigDTO;
import com.institute.achievement.reminder.dto.ReminderConfigVO;
import com.institute.achievement.reminder.service.ReminderConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for reminder configuration CRUD.
 * <p>
 * Provides endpoints for admin to manage reminder type configurations.
 * All endpoints require appropriate RBAC permissions (T-4-01).
 *
 * <h3>Security mitigations</h3>
 * <ul>
 *   <li>T-4-01: {@code @PreAuthorize("hasPermission('system:reminder:list')")} on GET endpoints</li>
 *   <li>T-4-01: {@code @PreAuthorize("hasPermission('system:reminder:create')")} on POST</li>
 *   <li>T-4-01: {@code @PreAuthorize("hasPermission('system:reminder:edit')")} on PUT</li>
 *   <li>T-4-01: {@code @PreAuthorize("hasPermission('system:reminder:remove')")} on DELETE</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/reminder/configs")
@RequiredArgsConstructor
public class ReminderConfigController {

    private final ReminderConfigService reminderConfigService;

    /**
     * Paginated list of reminder configurations.
     *
     * @param page     page number (1-based, default 1)
     * @param size     page size (default 20)
     * @param typeCode optional filter by type code
     * @return paginated config list
     */
    @GetMapping("/page")
    @PreAuthorize("hasPermission('system:reminder:list')")
    public Result<Page<ReminderConfigVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String typeCode) {
        Page<ReminderConfigVO> result = reminderConfigService.page(page, size, typeCode);
        return Result.success(result);
    }

    /**
     * Get a single reminder configuration by ID.
     *
     * @param id the config ID
     * @return the config with resolved display fields
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('system:reminder:list')")
    public Result<ReminderConfigVO> getById(@PathVariable Long id) {
        ReminderConfigVO vo = reminderConfigService.getById(id);
        return Result.success(vo);
    }

    /**
     * Create a new reminder configuration.
     *
     * @param dto the config data
     * @return success response
     */
    @PostMapping
    @PreAuthorize("hasPermission('system:reminder:create')")
    public Result<Void> create(@RequestBody @Valid ReminderConfigDTO dto) {
        reminderConfigService.create(dto);
        return Result.success();
    }

    /**
     * Update an existing reminder configuration.
     *
     * @param id  the config ID
     * @param dto the updated config data
     * @return success response
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('system:reminder:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid ReminderConfigDTO dto) {
        reminderConfigService.update(id, dto);
        return Result.success();
    }

    /**
     * Delete a reminder configuration.
     * <p>
     * Checks for pending tasks before allowing deletion (T-4-03).
     *
     * @param id the config ID to delete
     * @return success response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('system:reminder:remove')")
    public Result<Void> delete(@PathVariable Long id) {
        reminderConfigService.delete(id);
        return Result.success();
    }
}
