package com.institute.achievement.reminder.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.institute.achievement.reminder.dto.ReminderTaskVO;

import java.util.List;

/**
 * Service interface for reminder task management.
 * <p>
 * Covers task generation (scheduled batch), user-facing query/mutate
 * operations, and escalation processing.
 */
public interface ReminderTaskService {

    /**
     * Batch-generate reminder tasks from enabled configs.
     * <p>
     * Scans all enabled {@code reminder_config} entries, resolves target
     * users (personal + role-based per D-05), performs dedup checks,
     * inserts task records, and sends in-app notifications.
     *
     * @param today today's date string (YYYY-MM-DD), used for logging
     * @return number of tasks generated
     */
    int generateTasks(String today);

    /**
     * Paginated listing of tasks for a specific user.
     *
     * @param userId  the user ID
     * @param urgency optional urgency filter (HIGH/MEDIUM/LOW)
     * @param page    page number (1-based)
     * @param size    page size
     * @return paginated task list ordered by deadline ASC
     */
    IPage<ReminderTaskVO> listByUser(Long userId, String urgency, int page, int size);

    /**
     * Paginated listing of tasks for a specific config (admin view).
     *
     * @param configId the config ID
     * @param page     page number (1-based)
     * @param size     page size
     * @return paginated task list for this config
     */
    IPage<ReminderTaskVO> listByConfigId(Long configId, int page, int size);

    /**
     * Get a single task by ID with ownership verification.
     *
     * @param id the task ID
     * @return the task VO
     * @throws com.institute.achievement.common.exception.AchievementException if not found
     */
    ReminderTaskVO getById(Long id);

    /**
     * Confirm receipt of a reminder task (D-17).
     * <p>
     * Sets {@code confirmedFlag=1} and {@code confirmedTime=now}.
     * Validates that the task belongs to the specified user.
     *
     * @param taskId the task ID
     * @param userId the user ID confirming receipt
     * @throws com.institute.achievement.common.exception.AchievementException if not found or not authorized
     */
    void confirmReceipt(Long taskId, Long userId);

    /**
     * Dismiss a reminder task (frontend-only tracking).
     * <p>
     * No DB state change — the frontend maintains a dismissed IDs set
     * to avoid re-displaying high-urgency popups. The method exists
     * for the frontend to call; logs the dismiss action.
     *
     * @param taskId the task ID
     * @param userId the user ID dismissing the task
     */
    void dismissTask(Long taskId, Long userId);

    /**
     * Get unconfirmed HIGH urgency tasks with future deadlines for a user.
     * <p>
     * Used by the high-urgency global popup feature (D-20).
     * Returns at most 20 tasks, ordered by deadline ASC.
     *
     * @param userId the user ID
     * @return list of high-urgency unconfirmed tasks
     */
    List<ReminderTaskVO> getHighUrgencyUnconfirmed(Long userId);

    /**
     * Get count of unconfirmed tasks with future deadlines for a user.
     * <p>
     * Used for badge count display in the notification center.
     *
     * @param userId the user ID
     * @return count of unconfirmed tasks
     */
    long getUnconfirmedCount(Long userId);
}
