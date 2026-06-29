package com.institute.achievement.common.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    /**
     * Batch resolve user display names from user IDs.
     * <p>
     * Default implementation calls {@link #resolveUserName(Long)} per ID.
     * Implementations with database access should override to use a single
     * batch query for efficiency.
     *
     * @param userIds collection of user IDs
     * @return map of userId -> display name
     */
    default Map<Long, String> resolveUserNames(Collection<Long> userIds) {
        return userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(id -> id, this::resolveUserName));
    }
}
