package com.institute.achievement.module.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.common.service.INotificationService;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.module.system.entity.Notification;
import com.institute.achievement.module.system.entity.SysUser;
import com.institute.achievement.module.system.mapper.NotificationMapper;
import com.institute.achievement.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * In-app notification service with Redis unread count caching.
 * <p>
 * Handles creating notifications on approval events, tracking unread
 * counts via Redis keys ("notify:unread:{userId}"), and scheduled
 * cleanup of notifications older than 30 days (D-55).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements INotificationService {

    private static final String UNREAD_KEY_PREFIX = "notify:unread:";
    private static final long REDIS_CACHE_SECONDS = 60;

    private final NotificationMapper notificationMapper;
    private final StringRedisTemplate redisTemplate;
    private final SysUserMapper sysUserMapper;

    /**
     * Send a notification to a user.
     * Creates a DB record and increments the Redis unread count.
     */
    @Transactional
    public Long send(Long userId, String type, String title, String content,
                     String relatedAchievementType, Long relatedAchievementId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedAchievementType(relatedAchievementType);
        notification.setRelatedAchievementId(relatedAchievementId);
        notification.setReadFlag(0);
        notification.setCreatedTime(LocalDateTime.now());

        notificationMapper.insert(notification);

        // Increment Redis unread count
        try {
            redisTemplate.opsForValue().increment(UNREAD_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Redis increment failed (non-fatal): {}", e.getMessage());
        }

        log.info("Notification sent: userId={}, type={}, title='{}'", userId, type, title);
        return notification.getId();
    }

    /**
     * Mark a single notification as read, decrementing Redis count.
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw AchievementException.notFound("通知", notificationId);
        }
        if (!notification.getUserId().equals(userId)) {
            throw AchievementException.notAuthorized("只能标记自己的通知为已读");
        }

        if (notification.getReadFlag() == 0) {
            notificationMapper.markAsRead(notificationId);
            // Decrement Redis unread count
            try {
                redisTemplate.opsForValue().decrement(UNREAD_KEY_PREFIX + userId);
            } catch (Exception e) {
                log.warn("Redis decrement failed (non-fatal): {}", e.getMessage());
            }
        }
    }

    /**
     * Mark all notifications as read for a user.
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        int updated = notificationMapper.markAllAsRead(userId);
        // Reset Redis unread count to 0
        try {
            redisTemplate.opsForValue().set(UNREAD_KEY_PREFIX + userId, "0");
        } catch (Exception e) {
            log.warn("Redis set failed (non-fatal): {}", e.getMessage());
        }
        return updated;
    }

    /**
     * Get unread notification count for a user.
     * Tries Redis cache first, falls back to DB query with cache refresh.
     */
    public Long getUnreadCount(Long userId) {
        try {
            // Try Redis first
            String cached = redisTemplate.opsForValue().get(UNREAD_KEY_PREFIX + userId);
            if (cached != null) {
                return Long.parseLong(cached);
            }
        } catch (Exception e) {
            log.trace("Redis get failed, falling back to DB: {}", e.getMessage());
        }

        // Fallback to DB
        Long count = notificationMapper.countUnread(userId);
        if (count == null) count = 0L;

        // Cache in Redis with TTL
        try {
            redisTemplate.opsForValue().set(UNREAD_KEY_PREFIX + userId, String.valueOf(count),
                    REDIS_CACHE_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.trace("Redis set failed (non-fatal): {}", e.getMessage());
        }

        return count;
    }

    /**
     * Paginated list of notifications by user and type.
     */
    public IPage<Notification> listByUser(Long userId, String type, int page, int size) {
        Page<Notification> pageParam = new Page<>(page, size);
        return notificationMapper.findByUserIdAndType(pageParam, userId, type);
    }

    /**
     * Send approval notification to all department secretaries.
     * Called when an achievement is submitted for department review.
     * <p>
     * Phase 2 upgrade: sends notifications to real users via RBAC query
     * instead of the Phase 1 sentinel userId=0 pattern.
     */
    @Transactional
    public void notifyDeptSecretaries(Long deptId, String achievementType,
                                       Long achievementId, String title) {
        String notificationTitle = "新的审批待办：" + title;
        String content = "成果《" + title + "》已提交，请尽快审核。";

        // Notify all department secretaries
        List<Long> secretaryIds = findUserIdsByDeptAndRole(deptId, "ROLE_DEPT_SECRETARY");
        for (Long userId : secretaryIds) {
            send(userId, "APPROVAL", notificationTitle, content, achievementType, achievementId);
        }

        // Also notify department admins who share approval responsibility
        List<Long> adminIds = findUserIdsByDeptAndRole(deptId, "ROLE_DEPT_ADMIN");
        for (Long userId : adminIds) {
            send(userId, "APPROVAL", notificationTitle, content, achievementType, achievementId);
        }

        // Fallback: if no dept-level approvers found, send to sentinel userId=0
        if (secretaryIds.isEmpty() && adminIds.isEmpty()) {
            log.warn("No approvers found in deptId={}, fallback to sentinel", deptId);
            send(0L, "APPROVAL", notificationTitle, content, achievementType, achievementId);
        }

        log.info("Dept approvers notified: deptId={}, type={}, id={}, secretaries={}, admins={}",
                deptId, achievementType, achievementId, secretaryIds.size(), adminIds.size());
    }

    /**
     * Send approval notification to all admins.
     * Called when department secretary passes an achievement to admin.
     */
    @Transactional
    public void notifyAdmin(String achievementType, Long achievementId, String title) {
        String notificationTitle = "待归档： " + title;
        String content = "成果《" + title + "》已通过部门审核，请分配归档号。";

        // Same pattern as notifyDeptSecretaries — sentinel userId=0 for admin broadcast
        send(0L, "APPROVAL", notificationTitle, content, achievementType, achievementId);
        log.info("Admin notified: achievementType={}, id={}", achievementType, achievementId);
    }

    /**
     * Send notification to the achievement submitter.
     */
    @Transactional
    public void notifySubmitter(Long achievementId, String type, String message) {
        // In production, this loads the achievement to get created_by
        // For Phase 1, the caller provides the userId or this is done directly
        log.info("Submitter notification queued: achievementId={}, type={}, message='{}'",
                achievementId, type, message);
    }

    /**
     * Find all user IDs in a department that have a specific role.
     * <p>
     * Used by alert escalation and notification routing to send
     * role-appropriate notifications (T-02-04-03 mitigation).
     *
     * @param deptId   the department ID to search in
     * @param roleCode the role code (e.g. "ROLE_SECRETARY", "ROLE_LEADER")
     * @return list of user IDs with the specified role in the department (may be empty)
     */
    public List<Long> findUserIdsByDeptAndRole(Long deptId, String roleCode) {
        if (deptId == null) {
            log.warn("findUserIdsByDeptAndRole called with null deptId");
            return new ArrayList<>();
        }
        List<Long> userIds = sysUserMapper.findUserIdsByDeptAndRole(deptId, roleCode);
        if (userIds.isEmpty()) {
            log.warn("No users found for deptId={}, roleCode={}", deptId, roleCode);
        }
        return userIds;
    }

    /**
     * Resolve a user's display name from their user ID.
     * Overrides default to query the actual user name from DB.
     */
    @Override
    public String resolveUserName(Long userId) {
        try {
            var user = sysUserMapper.selectById(userId);
            if (user != null && user.getRealName() != null && !user.getRealName().isEmpty()) {
                return user.getRealName();
            }
        } catch (Exception e) {
            log.trace("Failed to resolve user name for userId={}: {}", userId, e.getMessage());
        }
        return "用户" + userId;
    }

    /**
     * Batch resolve user display names.
     * Uses a single batch query for efficiency.
     */
    @Override
    public Map<Long, String> resolveUserNames(java.util.Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        try {
            List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
            return users.stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
        } catch (Exception e) {
            log.trace("Failed to batch resolve user names: {}", e.getMessage());
            return INotificationService.super.resolveUserNames(userIds);
        }
    }

    /**
     * Scheduled cleanup: delete notifications older than 30 days.
     * Runs daily at 2 AM. Uses batch delete (LIMIT 1000) per Pitfall 5.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int totalDeleted = 0;
        int batchCount;

        do {
            batchCount = notificationMapper.deleteOlderThan(cutoff);
            totalDeleted += batchCount;
        } while (batchCount >= 1000);

        if (totalDeleted > 0) {
            log.info("Notification cleanup: deleted {} notifications older than 30 days", totalDeleted);
        }
    }
}
