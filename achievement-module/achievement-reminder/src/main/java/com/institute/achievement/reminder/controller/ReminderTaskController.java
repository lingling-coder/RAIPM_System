package com.institute.achievement.reminder.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.institute.achievement.common.exception.AchievementException;
import com.institute.achievement.common.util.Result;
import com.institute.achievement.framework.security.SecurityUtils;
import com.institute.achievement.reminder.dto.ReminderTaskVO;
import com.institute.achievement.reminder.service.ReminderTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user-facing reminder task operations (D-07, D-08, D-17, D-20).
 * <p>
 * Provides endpoints for users to list their tasks, view details,
 * confirm receipt, dismiss tasks, and query high-urgency tasks for
 * the global popup feature.
 * <p>
 * All endpoints derive the current user ID from
 * {@link SecurityUtils#getCurrentUserId()} and the service layer
 * verifies ownership before mutations.
 *
 * <h3>Security mitigations</h3>
 * <ul>
 *   <li>T-4-03: All endpoints derive userId from SecurityUtils; service verifies ownership</li>
 *   <li>T-4-05: Ownership check before confirm/dismiss mutation (service layer)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/reminder/tasks")
@RequiredArgsConstructor
public class ReminderTaskController {

    private final ReminderTaskService reminderTaskService;

    /**
     * Paginated listing of the current user's reminder tasks.
     *
     * @param page    page number (1-based, default 1)
     * @param size    page size (default 20)
     * @param urgency optional urgency filter (HIGH/MEDIUM/LOW)
     * @return paginated task list ordered by deadline ASC
     */
    @GetMapping("/page")
    public Result<IPage<ReminderTaskVO>> pageTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String urgency) {
        Long userId = SecurityUtils.getCurrentUserId();
        IPage<ReminderTaskVO> result = reminderTaskService.listByUser(userId, urgency, page, size);
        return Result.success(result);
    }

    /**
     * Get a single task by ID with ownership verification.
     *
     * @param id the task ID
     * @return the task detail
     */
    @GetMapping("/{id}")
    public Result<ReminderTaskVO> getTaskById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReminderTaskVO vo = reminderTaskService.getById(id);
        // Ownership verification (T-4-03)
        if (!vo.getUserId().equals(userId)) {
            throw AchievementException.notAuthorized("只能查看自己的提醒任务");
        }
        return Result.success(vo);
    }

    /**
     * Confirm receipt of a reminder task (D-17).
     * <p>
     * Records a formal read receipt with timestamp.
     * Service layer verifies ownership.
     *
     * @param id the task ID to confirm
     * @return success response
     */
    @PutMapping("/{id}/confirm")
    public Result<Void> confirmReceipt(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        reminderTaskService.confirmReceipt(id, userId);
        return Result.success();
    }

    /**
     * Dismiss a reminder task (frontend-only tracking for high-urgency popup).
     * <p>
     * Logs the dismiss action for audit. No DB state change —
     * the frontend maintains a dismissed IDs set to avoid re-displaying
     * the same task in the high-urgency popup.
     *
     * @param id the task ID to dismiss
     * @return success response
     */
    @PostMapping("/{id}/dismiss")
    public Result<Void> dismissTask(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        reminderTaskService.dismissTask(id, userId);
        return Result.success();
    }

    /**
     * Get unconfirmed HIGH urgency tasks for the current user (D-20).
     * <p>
     * Used by the frontend to fetch tasks for the high-urgency global
     * popup. Returns tasks where urgency=HIGH, confirmedFlag=0, and
     * deadline >= today, ordered by deadline ASC (max 20).
     *
     * @return list of high-urgency unconfirmed tasks
     */
    @GetMapping("/high-urgency-unconfirmed")
    public Result<List<ReminderTaskVO>> getHighUrgencyUnconfirmed() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<ReminderTaskVO> tasks = reminderTaskService.getHighUrgencyUnconfirmed(userId);
        return Result.success(tasks);
    }

    /**
     * Get count of unconfirmed tasks with future deadlines.
     * <p>
     * Used by the notification center to display a badge count
     * for the REMINDER tab.
     *
     * @return unconfirmed task count
     */
    @GetMapping("/unconfirmed-count")
    public Result<Long> getUnconfirmedCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        long count = reminderTaskService.getUnconfirmedCount(userId);
        return Result.success(count);
    }
}
