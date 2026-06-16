package com.institute.achievement.framework.permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable data-level permission filtering on mapper methods.
 * When placed on a MyBatis-Plus mapper method, the DataPermissionInterceptor
 * will inject a dept_id filter clause into the SQL query.
 *
 * Usage:
 * {@code @DataScope(deptAlias = "t")}
 * {@code @DataScope(deptAlias = "u", includeClassified = true)}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /**
     * Table alias for the dept_id column in the query.
     * Defaults to "t" for single-table queries.
     * For JOIN queries, set to the alias of the main entity table.
     */
    String deptAlias() default "t";

    /**
     * Whether to include classified data in the query results.
     * Defaults to false (classified data excluded unless user has special role).
     */
    boolean includeClassified() default false;
}
