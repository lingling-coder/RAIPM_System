package com.institute.achievement.module.system.controller;

import com.institute.achievement.common.util.Result;
import com.institute.achievement.framework.security.JwtUser;
import com.institute.achievement.module.system.dto.PasswordChangeDTO;
import com.institute.achievement.module.system.dto.ProfileUpdateDTO;
import com.institute.achievement.module.system.dto.ProfileVO;
import com.institute.achievement.module.system.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Profile controller for personal settings.
 * Self-service endpoints: view profile, edit personal info, change password (D-16).
 */
@RestController
@RequestMapping("/api/system/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Get the authenticated user's profile.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<ProfileVO> getProfile(@AuthenticationPrincipal JwtUser jwtUser) {
        if (jwtUser == null) {
            return Result.unauthorized("未认证");
        }
        return Result.success(profileService.getProfile(jwtUser.getUserId()));
    }

    /**
     * Update personal profile information.
     * Users can update: name, email, phone only (D-16).
     */
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Void> updateProfile(@AuthenticationPrincipal JwtUser jwtUser,
                                       @Valid @RequestBody ProfileUpdateDTO dto) {
        if (jwtUser == null) {
            return Result.unauthorized("未认证");
        }
        profileService.updateProfile(jwtUser.getUserId(), dto);
        return Result.success();
    }

    /**
     * Change password with old password verification.
     * On success, blacklists all tokens to force re-login (D-16).
     */
    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> changePassword(@AuthenticationPrincipal JwtUser jwtUser,
                                        @Valid @RequestBody PasswordChangeDTO dto) {
        if (jwtUser == null) {
            return Result.unauthorized("未认证");
        }
        profileService.changePassword(jwtUser.getUserId(), dto);
        return Result.success();
    }
}
