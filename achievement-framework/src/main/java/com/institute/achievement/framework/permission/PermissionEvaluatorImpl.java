package com.institute.achievement.framework.permission;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom PermissionEvaluator for Spring Security @PreAuthorize.
 * Supports hasPermission() expressions in method security.
 *
 * Usage:
 * {@code @PreAuthorize("hasPermission(null, 'system:user:list')")}
 * {@code @PreAuthorize("hasPermission(#id, 'system:user:edit')")}
 *
 * Permissions are stored in JWT as "PERM_{permission}" authorities.
 * ROLE_SYSTEM_ADMIN bypasses all permission checks.
 */
@Slf4j
@Component
public class PermissionEvaluatorImpl implements PermissionEvaluator {

    /**
     * Check if the user has a specific permission.
     * targetDomainObject is ignored (we check against the permission string directly).
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Get user's authorities
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // Admin bypass: ROLE_SYSTEM_ADMIN has all permissions
        if (authorities.contains("ROLE_SYSTEM_ADMIN")) {
            return true;
        }

        if (permission instanceof String permissionStr) {
            // Check both raw permission and PERM_ prefixed version
            String permPrefixed = permissionStr.startsWith("PERM_") ? permissionStr : "PERM_" + permissionStr;
            return authorities.contains(permissionStr) || authorities.contains(permPrefixed);
        }

        return false;
    }

    /**
     * Check if the user has a specific permission on a target ID.
     * Delegates to hasPermission(Authentication, Object, Object).
     */
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId,
                                 String targetType, Object permission) {
        return hasPermission(authentication, targetType, permission);
    }
}
