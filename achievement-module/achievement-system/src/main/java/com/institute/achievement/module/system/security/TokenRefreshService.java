package com.institute.achievement.module.system.security;

import com.institute.achievement.common.exception.BusinessException;
import com.institute.achievement.framework.security.JwtTokenProvider;
import com.institute.achievement.module.system.entity.SysMenu;
import com.institute.achievement.module.system.entity.SysRole;
import com.institute.achievement.module.system.entity.SysUser;
import com.institute.achievement.module.system.mapper.SysMenuMapper;
import com.institute.achievement.module.system.mapper.SysRoleMapper;
import com.institute.achievement.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Token refresh service.
 * Validates refresh tokens and issues new access tokens.
 * On password change, blacklists all tokens to force re-login (D-16).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRefreshService {

    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;

    /**
     * Validate refresh token and generate a new access token.
     */
    public String refreshAccessToken(String refreshToken) {
        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("Invalid or expired refresh token");
        }
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException("Token is not a refresh token");
        }

        // Extract user ID
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        if (userId == null) {
            throw new BusinessException("Invalid token payload");
        }

        // Re-fetch user from DB to re-check status
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }
        if (user.getDeleted() != null && user.getDeleted() == 1) {
            throw new BusinessException("Account has been deleted");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BusinessException("Account has been disabled");
        }

        // Load current roles and permissions
        List<SysRole> roles = roleMapper.selectRolesByUserId(userId);
        List<String> roleCodes = roles.stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());

        List<SysMenu> menus = menuMapper.selectMenusByUserId(userId);
        List<String> permissions = menus.stream()
                .map(SysMenu::getPermission)
                .filter(p -> p != null && !p.isEmpty())
                .collect(Collectors.toList());

        // Generate new access token
        return jwtTokenProvider.generateAccessToken(userId, roleCodes, user.getDeptId(), permissions);
    }

    /**
     * Blacklist a refresh token.
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeRefreshToken(String refreshToken) {
        Duration remaining = jwtTokenProvider.getRemainingDuration(refreshToken);
        if (!remaining.isNegative() && !remaining.isZero()) {
            jwtTokenProvider.blacklistToken(refreshToken, remaining);
        }
    }

    /**
     * Blacklist all tokens for a user (used on password change).
     */
    @Transactional(rollbackFor = Exception.class)
    public void blacklistAllUserTokens(Long userId, String currentAccessToken, String currentRefreshToken) {
        if (currentAccessToken != null) {
            Duration accessRemaining = jwtTokenProvider.getRemainingDuration(currentAccessToken);
            if (!accessRemaining.isNegative() && !accessRemaining.isZero()) {
                jwtTokenProvider.blacklistToken(currentAccessToken, accessRemaining);
            }
        }
        if (currentRefreshToken != null) {
            Duration refreshRemaining = jwtTokenProvider.getRemainingDuration(currentRefreshToken);
            if (!refreshRemaining.isNegative() && !refreshRemaining.isZero()) {
                jwtTokenProvider.blacklistToken(currentRefreshToken, refreshRemaining);
            }
        }
        log.info("All tokens blacklisted for userId: {}", userId);
    }
}
