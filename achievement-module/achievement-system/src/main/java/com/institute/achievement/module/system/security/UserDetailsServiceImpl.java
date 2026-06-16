package com.institute.achievement.module.system.security;

import com.institute.achievement.module.system.entity.SysMenu;
import com.institute.achievement.module.system.entity.SysRole;
import com.institute.achievement.module.system.entity.SysUser;
import com.institute.achievement.module.system.mapper.SysMenuMapper;
import com.institute.achievement.module.system.mapper.SysRoleMapper;
import com.institute.achievement.module.system.mapper.SysUserMapper;
import com.institute.achievement.framework.security.JwtUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UserDetailsService implementation that loads user data from the database.
 * Checks user status, deletion flag, and account lockout during authentication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        // Check soft delete (D-20)
        if (user.getDeleted() != null && user.getDeleted() == 1) {
            throw new org.springframework.security.authentication.DisabledException("账户已删除");
        }

        // Check enable/disable status (D-19)
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new org.springframework.security.authentication.DisabledException("账户已停用");
        }

        // Check account lockout (D-15)
        if (user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(LocalDateTime.now())) {
            long remainingMinutes = java.time.Duration.between(LocalDateTime.now(), user.getLockoutUntil()).toMinutes();
            throw new org.springframework.security.authentication.LockedException(
                    "账户已锁定，请" + remainingMinutes + "分钟后再试");
        }

        // Load roles
        List<SysRole> roles = roleMapper.selectRolesByUserId(user.getId());
        List<String> roleCodes = roles.stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());

        // Load permissions (menu permissions)
        List<SysMenu> menus = menuMapper.selectMenusByUserId(user.getId());
        List<String> permissions = menus.stream()
                .map(SysMenu::getPermission)
                .filter(p -> p != null && !p.isEmpty())
                .collect(Collectors.toList());

        // Build authorities
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (roleCodes != null) {
            roleCodes.forEach(role -> authorities.add(
                    new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role)));
        }
        if (permissions != null) {
            permissions.forEach(perm -> authorities.add(new SimpleGrantedAuthority("PERM_" + perm)));
        }

        log.debug("User '{}' loaded with {} roles and {} permissions",
                username, roleCodes.size(), permissions.size());

        // Add selectByUsername to mapper via default method or use LambdaQueryWrapper
        // We'll use the mapper directly
        return new JwtUser(
                user.getId(),
                user.getPassword(),
                user.getDeptId(),
                roleCodes,
                permissions,
                authorities
        );
    }
}
