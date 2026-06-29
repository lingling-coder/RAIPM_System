package com.institute.achievement.framework.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;

/**
 * Static helper for extracting security context and request information.
 * <p>
 * Provides convenient access to the currently authenticated user's details
 * and the client IP address, used by the audit logging AOP aspect and
 * other security-sensitive components.
 */
@Slf4j
public final class SecurityUtils {

    private SecurityUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Get the username of the currently authenticated user.
     *
     * @return username, or "anonymous" if no authentication is available
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof JwtUser) {
            // JwtUser stores userId as getUsername(), but we need the actual username.
            // Retrieve it from the authentication name instead.
            return authentication.getName();
        }
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }

    /**
     * Get the user ID of the currently authenticated user.
     *
     * @return user ID, or null if not authenticated
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof JwtUser) {
            return ((JwtUser) authentication.getPrincipal()).getUserId();
        }
        return null;
    }

    /**
     * Get the department ID of the currently authenticated user.
     *
     * @return department ID, or null if not available
     */
    public static Long getCurrentDeptId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof JwtUser) {
            return ((JwtUser) authentication.getPrincipal()).getDeptId();
        }
        return null;
    }

    /**
     * Get the list of roles for the currently authenticated user.
     *
     * @return list of role names, or empty list if not authenticated
     */
    public static List<String> getCurrentRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof JwtUser) {
            return ((JwtUser) authentication.getPrincipal()).getRoles();
        }
        return Collections.emptyList();
    }

    /**
     * Get the list of permissions for the currently authenticated user.
     *
     * @return list of permission identifiers, or empty list if not authenticated
     */
    public static List<String> getCurrentPermissions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof JwtUser) {
            return ((JwtUser) authentication.getPrincipal()).getPermissions();
        }
        return Collections.emptyList();
    }

    /**
     * Get the client IP address from the current HTTP request.
     * <p>
     * Respects X-Forwarded-For header for reverse proxy scenarios.
     *
     * @return client IP address string, or "unknown" if not determinable
     */
    public static String getClientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }
            HttpServletRequest request = attributes.getRequest();

            // Check X-Forwarded-For first (proxy support)
            String ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain a comma-separated list; take the first
                return ip.split(",")[0].trim();
            }

            // Check other common proxy headers
            ip = request.getHeader("X-Real-IP");
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }

            ip = request.getHeader("Proxy-Client-IP");
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }

            ip = request.getHeader("WL-Proxy-Client-IP");
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }

            // Fall back to remote address
            return request.getRemoteAddr();

        } catch (Exception e) {
            log.trace("Could not determine client IP: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * Check if the current user has a specific permission.
     *
     * @param permission the permission identifier (without PERM_ prefix)
     * @return true if the user has the permission
     */
    public static boolean hasPermission(String permission) {
        List<String> permissions = getCurrentPermissions();
        return permissions != null && permissions.contains(permission);
    }

    /**
     * Check if the current user has a specific role.
     * <p>
     * Supports three matching strategies:
     * <ol>
     *   <li>Exact match against the raw role string</li>
     *   <li>Exact match with {@code ROLE_} prefix added automatically if missing</li>
     *   <li>Underscore-prefixed uppercase suffix match — e.g. {@code "secretary"}
     *       matches {@code ROLE_DEPT_SECRETARY} via {@code _SECRETARY} suffix</li>
     * </ol>
     *
     * @param role the role name (without ROLE_ prefix)
     * @return true if the user has the role
     */
    public static boolean hasRole(String role) {
        List<String> roles = getCurrentRoles();
        if (roles == null) {
            return false;
        }
        // Direct match
        if (roles.contains(role)) {
            return true;
        }
        // Match with ROLE_ prefix
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        if (roles.contains(roleWithPrefix)) {
            return true;
        }
        // Suffix match: e.g. "secretary" → "_SECRETARY" matches ROLE_DEPT_SECRETARY
        String suffix = "_" + role.toUpperCase();
        return roles.stream().anyMatch(r -> r.toUpperCase().endsWith(suffix));
    }
}
