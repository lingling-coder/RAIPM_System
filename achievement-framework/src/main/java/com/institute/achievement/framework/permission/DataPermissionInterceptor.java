package com.institute.achievement.framework.permission;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;

/**
 * MyBatis-Plus InnerInterceptor that injects dept_id filter on @DataScope-annotated mapper methods.
 * Uses jsqlparser to safely modify the SQL WHERE clause.
 * Admin role (ROLE_SYSTEM_ADMIN) bypasses the filter.
 */
@Slf4j
public class DataPermissionInterceptor implements InnerInterceptor {

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        // Check if mapped method has @DataScope annotation
        DataScope dataScope = getDataScopeAnnotation(ms);
        if (dataScope == null) {
            // No annotation, no filtering
            return;
        }

        // Get current user from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        // Admin bypass: ROLE_SYSTEM_ADMIN sees all
        if (hasRole(authentication, "ROLE_SYSTEM_ADMIN")) {
            return;
        }

        // Extract user's dept_id from the authentication principal
        Long deptId = extractDeptId(authentication);
        if (deptId == null) {
            log.debug("User has no dept_id set, skipping data permission filter");
            return;
        }

        // Modify the SQL to inject dept_id filter
        String originalSql = boundSql.getSql();
        try {
            String modifiedSql = injectDeptFilter(originalSql, dataScope.deptAlias(), deptId);
            PluginUtils.mpBoundSql(boundSql).sql(modifiedSql);
            log.debug("Data permission injected: {} alias={} deptId={}", ms.getId(), dataScope.deptAlias(), deptId);
        } catch (JSQLParserException e) {
            log.error("Failed to parse SQL for data permission injection: {}", e.getMessage());
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    private DataScope getDataScopeAnnotation(MappedStatement ms) {
        try {
            String className = ms.getId().substring(0, ms.getId().lastIndexOf('.'));
            String methodName = ms.getId().substring(ms.getId().lastIndexOf('.') + 1);

            Class<?> clazz = Class.forName(className);
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName) && method.isAnnotationPresent(DataScope.class)) {
                    return method.getAnnotation(DataScope.class);
                }
            }
        } catch (ClassNotFoundException e) {
            log.debug("Class not found for data scope resolution: {}", ms.getId());
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private String injectDeptFilter(String sql, String alias, Long deptId) throws JSQLParserException {
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();

        // Build: {alias}.dept_id = {deptId}
        Column deptColumn = new Column(alias + ".dept_id");
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(deptColumn);
        equalsTo.setRightExpression(new LongValue(deptId));

        // Add to existing WHERE clause
        if (plainSelect.getWhere() == null) {
            plainSelect.setWhere(equalsTo);
        } else {
            plainSelect.setWhere(new AndExpression(plainSelect.getWhere(), equalsTo));
        }

        return select.toString();
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

    private Long extractDeptId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.institute.achievement.framework.security.JwtUser) {
            return ((com.institute.achievement.framework.security.JwtUser) principal).getDeptId();
        }
        return null;
    }
}
