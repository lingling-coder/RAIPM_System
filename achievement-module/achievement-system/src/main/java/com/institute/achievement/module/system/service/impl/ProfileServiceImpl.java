package com.institute.achievement.module.system.service.impl;

import com.institute.achievement.common.exception.BadRequestException;
import com.institute.achievement.common.exception.EntityNotFoundException;
import com.institute.achievement.module.system.dto.PasswordChangeDTO;
import com.institute.achievement.module.system.dto.ProfileUpdateDTO;
import com.institute.achievement.module.system.dto.ProfileVO;
import com.institute.achievement.module.system.entity.SysDepartment;
import com.institute.achievement.module.system.entity.SysRole;
import com.institute.achievement.module.system.entity.SysUser;
import com.institute.achievement.module.system.mapper.SysDepartmentMapper;
import com.institute.achievement.module.system.mapper.SysRoleMapper;
import com.institute.achievement.module.system.mapper.SysUserMapper;
import com.institute.achievement.module.system.security.TokenRefreshService;
import com.institute.achievement.module.system.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Profile service implementation.
 * Handles profile viewing, editing, and password changes (D-16).
 * Password change forces re-login by blacklisting all user tokens.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDepartmentMapper departmentMapper;
    private final TokenRefreshService tokenRefreshService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public ProfileVO getProfile(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || (user.getDeleted() != null && user.getDeleted() == 1)) {
            throw new EntityNotFoundException("User", userId);
        }

        ProfileVO.ProfileVOBuilder builder = ProfileVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .deptId(user.getDeptId())
                .lastLoginIp(user.getLastLoginIp())
                .lastLoginTime(user.getLastLoginTime())
                .passwordChangeRequired(user.getPasswordChangeRequired() != null && user.getPasswordChangeRequired() == 1);

        // Resolve department name
        if (user.getDeptId() != null) {
            SysDepartment dept = departmentMapper.selectById(user.getDeptId());
            if (dept != null) {
                builder.deptName(dept.getDeptName());
            }
        }

        // Resolve role names
        List<SysRole> roles = roleMapper.selectRolesByUserId(user.getId());
        if (roles != null) {
            builder.roleNames(roles.stream()
                    .map(SysRole::getRoleName)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    @Override
    public void updateProfile(Long userId, ProfileUpdateDTO dto) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || (user.getDeleted() != null && user.getDeleted() == 1)) {
            throw new EntityNotFoundException("User", userId);
        }

        // Update only allowed fields (name, email, phone) per D-16
        if (dto.getRealName() != null) {
            user.setRealName(dto.getRealName());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }

        userMapper.updateById(user);
        log.info("Profile updated for userId: {}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, PasswordChangeDTO dto) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || (user.getDeleted() != null && user.getDeleted() == 1)) {
            throw new EntityNotFoundException("User", userId);
        }

        // Verify old password
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("旧密码验证失败");
        }

        // Validate new password matches confirm
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BadRequestException("两次输入的密码不一致");
        }

        // Validate password policy (D-12: min 8 + letter + number)
        if (dto.getNewPassword().length() < 8 ||
                !dto.getNewPassword().matches("^(?=.*[a-zA-Z])(?=.*\\d).+$")) {
            throw new BadRequestException("密码必须至少8位，且包含字母和数字");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setPasswordChangeRequired(0);
        userMapper.updateById(user);

        // Blacklist all existing tokens to force re-login (D-16)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String currentToken = extractJwtFromAuth(auth);
            tokenRefreshService.blacklistAllUserTokens(userId, currentToken, null);
        }

        log.info("Password changed for userId: {}, token blacklisted for re-login", userId);
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    private String extractJwtFromAuth(Authentication authentication) {
        if (authentication.getCredentials() instanceof String cred) {
            return cred;
        }
        return null;
    }
}
