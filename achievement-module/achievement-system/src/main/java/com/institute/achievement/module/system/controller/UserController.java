package com.institute.achievement.module.system.controller;

import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.common.util.Result;
import com.institute.achievement.module.system.dto.*;
import com.institute.achievement.module.system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * User management REST controller.
 * Provides CRUD operations, CSV import/export, password reset, and status management.
 *
 * All endpoints prefixed with /api/system/user per API convention.
 */
@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Paginated user list with filters.
     */
    @PostMapping("/page")
    public Result<PageResult<UserVO>> page(@RequestBody UserPageDTO dto) {
        return Result.success(userService.page(dto));
    }

    /**
     * Get user by ID.
     */
    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    /**
     * Create new user.
     */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody UserCreateDTO dto) {
        userService.create(dto);
        return Result.success();
    }

    /**
     * Update user.
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        dto.setId(id);
        userService.update(dto);
        return Result.success();
    }

    /**
     * Soft delete user (D-20).
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    /**
     * Batch soft delete users.
     */
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        userService.batchDelete(ids);
        return Result.success();
    }

    /**
     * Set user enable/disable status (D-19).
     */
    @PutMapping("/{id}/status")
    public Result<Void> setStatus(@PathVariable Long id, @RequestBody Integer status) {
        userService.setStatus(id, status);
        return Result.success();
    }

    /**
     * Reset user password to default (D-14).
     */
    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.success();
    }

    /**
     * Import users from CSV file (D-09).
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ImportResult> importCsv(@RequestParam("file") MultipartFile file) {
        ImportResult result = userService.importCsv(file);
        return Result.success(result);
    }
}
