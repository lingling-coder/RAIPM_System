package com.institute.achievement.module.system.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.institute.achievement.module.system.entity.SysUser;
import com.institute.achievement.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Account lockout service per D-15.
 * Tracks consecutive login failures and locks accounts after 5 failures for 30 minutes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLockoutService {

    private static final int MAX_FAILURES = 5;
    private static final int LOCKOUT_MINUTES = 30;

    private final SysUserMapper userMapper;

    /**
     * Record a login failure for the given username.
     * If failures >= 5, lock the account for 30 minutes.
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordLoginFailure(String username) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            log.warn("Login failure recorded for non-existent user: {}", username);
            return;
        }

        int failures = (user.getLoginFailures() != null ? user.getLoginFailures() : 0) + 1;
        user.setLoginFailures(failures);

        if (failures >= MAX_FAILURES) {
            user.setLockoutUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
            user.setLoginFailures(0); // Reset counter after lockout
            log.warn("Account '{}' locked for {} minutes due to {} consecutive failures",
                    username, LOCKOUT_MINUTES, MAX_FAILURES);
        }

        userMapper.updateById(user);
    }

    /**
     * Reset login attempts on successful authentication.
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetLoginAttempts(String username) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) return;

        user.setLoginFailures(0);
        user.setLockoutUntil(null);
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.debug("Login attempts reset for user: {}", username);
    }

    /**
     * Check if an account is currently locked.
     */
    public boolean isAccountLocked(String username) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) return false;

        return user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(LocalDateTime.now());
    }

    /**
     * Get remaining lockout minutes for an account.
     */
    public long getRemainingLockoutMinutes(String username) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null || user.getLockoutUntil() == null) return 0;

        long minutes = java.time.Duration.between(LocalDateTime.now(), user.getLockoutUntil()).toMinutes();
        return Math.max(0, minutes);
    }
}
