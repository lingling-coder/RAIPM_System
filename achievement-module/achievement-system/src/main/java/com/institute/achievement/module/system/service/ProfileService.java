package com.institute.achievement.module.system.service;

import com.institute.achievement.module.system.dto.PasswordChangeDTO;
import com.institute.achievement.module.system.dto.ProfileUpdateDTO;
import com.institute.achievement.module.system.dto.ProfileVO;

/**
 * Profile service for personal profile operations.
 * Supports viewing profile, updating personal info, and changing password (D-16).
 */
public interface ProfileService {

    /**
     * Get the authenticated user's profile information.
     */
    ProfileVO getProfile(Long userId);

    /**
     * Update personal profile information (name, email, phone only).
     */
    void updateProfile(Long userId, ProfileUpdateDTO dto);

    /**
     * Change password with old password verification.
     * Forces re-login on success by blacklisting user tokens.
     */
    void changePassword(Long userId, PasswordChangeDTO dto);
}
