package com.institute.achievement.common.service;

import java.util.List;

/**
 * Shared notification service interface.
 * <p>
 * Implemented by the system module's {@code NotificationService} and consumed
 * by other modules (e.g. fee module) to send notifications and query user IDs
 * without introducing circular Maven dependencies.
 */
public interface INotificationService {

    /**
     * Send a notification to a user.
     *
     * @param userId                 recipient user ID
     * @param type                   notification type code
     * @param title                  notification title
     * @param content                notification body
     * @param relatedAchievementType achievement type (paper|patent|software)
     * @param relatedAchievementId   related record ID
     * @return created notification ID
     */
    Long send(Long userId, String type, String title, String content,
              String relatedAchievementType, Long relatedAchievementId);

    /**
     * Find user IDs by department and role code.
     *
     * @param deptId   department ID filter
     * @param roleCode role code (e.g. "DEPT_ADMIN")
     * @return list of matching user IDs
     */
    List<Long> findUserIdsByDeptAndRole(Long deptId, String roleCode);

    /**
     * Resolve a user's display name from their user ID.
     * <p>
     * Default implementation returns a generic label; modules with access to
     * the user table (e.g. achievement-system) should override to return
     * the real name.
     *
     * @param userId the user ID
     * @return the display name
     */
    default String resolveUserName(Long userId) {
        return "用户" + userId;
    }
}
