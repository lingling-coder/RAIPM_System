package com.institute.achievement.framework.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus meta object handler for automatic audit field population.
 * <p>
 * Auto-fills created_at, created_by, updated_at, updated_by on INSERT and UPDATE operations.
 * Falls back to "SYSTEM" when no authenticated user is available (e.g., during seed data migration).
 */
@Slf4j
@Component
public class MyBatisPlusMetaHandler implements MetaObjectHandler {

    private static final String SYSTEM_USER = "SYSTEM";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        String currentUser = getCurrentUsername();

        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createdBy", String.class, currentUser);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedBy", String.class, currentUser);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        String currentUser = getCurrentUsername();

        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);
        this.strictUpdateFill(metaObject, "updatedBy", String.class, currentUser);
    }

    /**
     * Retrieve the current authenticated username from Spring Security context.
     * Falls back to "SYSTEM" when no authentication is available.
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("Could not retrieve authentication for meta fill: {}", e.getMessage());
        }
        return SYSTEM_USER;
    }
}
