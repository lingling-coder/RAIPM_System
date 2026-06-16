package com.institute.achievement.module.system.service;

import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.module.system.dto.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * User management service interface.
 * Handles CRUD, multi-role assignment, password management, CSV import/export.
 */
public interface UserService {

    /**
     * Paginated user list with filters.
     */
    PageResult<UserVO> page(UserPageDTO dto);

    /**
     * Get user by ID.
     */
    UserVO getById(Long id);

    /**
     * Create new user with password validation and role assignment.
     */
    void create(UserCreateDTO dto);

    /**
     * Update user fields and role assignments.
     */
    void update(UserUpdateDTO dto);

    /**
     * Soft delete user (D-20).
     */
    void delete(Long id);

    /**
     * Batch soft delete users.
     */
    void batchDelete(java.util.List<Long> ids);

    /**
     * Set user enable/disable status (D-19).
     */
    void setStatus(Long id, Integer status);

    /**
     * Reset user password to default (D-14).
     */
    void resetPassword(Long id);

    /**
     * Import users from CSV file (D-09: match by username, overwrite if exists).
     */
    ImportResult importCsv(MultipartFile file);
}
