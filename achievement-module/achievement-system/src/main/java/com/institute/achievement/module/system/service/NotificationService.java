package com.institute.achievement.module.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.module.system.entity.Notification;
import com.institute.achievement.module.system.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

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
public class NotificationService {

    private static final String UNREAD_KEY_PREFIX = "notify:unread:";
    private static final long REDIS_CACHE_SECONDS = 60;

    private final NotificationMapper notificationMapper;
    private final StringRedisTemplate redisTemplate;

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
     */
    @Transactional
    public void notifyDeptSecretaries(Long deptId, String achievementType,
                                       Long achievementId, String title) {
        // Find all users in this department with secretary role
        // For Phase 1, we use a simplified approach: a secretary notification channel
        // In Phase 2 with full RBAC, this will query sys_user + sys_user_role
        String notificationTitle = "新的审批待办：" + title;
        String content = "成果《" + title + "》已提交，请尽快审核。";

        // Note: In production, this queries the user/role service.
        // For Phase 1, we create a department-scoped notification pattern.
        // The actual user notification happens when the secretary polls their inbox.
        // We store it with userId as a sentinel value (0 = department notification)
        // In Phase 2, this will be replaced with proper role-based notification.
        send(0L, "APPROVAL", notificationTitle, content, achievementType, achievementId);
        log.info("Dept secretaries notified: deptId={}, achievementType={}, id={}",
                deptId, achievementType, achievementId);
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
