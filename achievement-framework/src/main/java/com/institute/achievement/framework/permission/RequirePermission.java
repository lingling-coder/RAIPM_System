package com.institute.achievement.framework.permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for method-level permission checking.
 * Methods annotated with this will be intercepted by PermissionAspect
 * to verify the current user has the specified permission.
 *
 * Usage:
 * {@code @RequirePermission("system:user:list")}
 * {@code @RequirePermission(value = "system:user:delete", requireAll = true)}
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * The permission identifier(s) to check.
     * Multiple values are OR-ed by default (user needs any one).
     */
    String[] value();

    /**
     * If true, user must have ALL specified permissions (AND logic).
     * If false (default), user needs ANY one permission (OR logic).
     */
    boolean requireAll() default false;
}
