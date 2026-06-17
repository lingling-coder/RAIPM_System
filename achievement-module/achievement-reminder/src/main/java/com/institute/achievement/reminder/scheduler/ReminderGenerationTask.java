package com.institute.achievement.reminder.scheduler;

import com.institute.achievement.reminder.service.ReminderTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Daily scheduled task for reminder task generation (D-02).
 * <p>
 * Runs daily at 3 AM, scans all enabled reminder configs and generates
 * reminder_task records for each configured user with deadline calculation,
 * template variable substitution, and in-app notification delivery.
 * <p>
 * Uses the same Redis distributed lock + idempotency key pattern as
 * {@code FeePlanGenerationTask} from Phase 2 to ensure at-most-once
 * execution across multiple instances.
 *
 * <h3>Threat model alignment</h3>
 * <ul>
 *   <li>T-4-02: UNIQUE INDEX (config_id, user_id, deadline) + pre-generation dedup query</li>
 *   <li>T-4-04: Redis distributed lock prevents concurrent execution; idempotency key prevents duplicate runs on restart</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderGenerationTask {

    private static final String LOCK_KEY = "spring:lock:reminder-gen";
    private static final long LOCK_TTL_SECONDS = 300;

    private final StringRedisTemplate redisTemplate;
    private final ReminderTaskService reminderTaskService;

    /**
     * Daily reminder task generation at 3 AM.
     * <p>
     * Idempotency: checks Redis key {@code "reminder:gen:last-run:{today}"} before executing.
     * Only proceeds if no record of today's run exists.
     * Lock TTL: 5 minutes (generation should complete in seconds for typical config counts).
     * <p>
     * On failure the idempotency key is deleted so the task retries the next day.
     * The distributed lock is always released in the {@code finally} block.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void generateReminderTasks() {
        String today = LocalDate.now().toString();

        // Idempotency check: skip if already run today
        Boolean alreadyRun = redisTemplate.opsForValue().setIfAbsent(
                "reminder:gen:last-run:" + today, "running",
                Duration.ofHours(2));
        if (Boolean.FALSE.equals(alreadyRun)) {
            log.debug("Reminder generation already executed for date {}, skipping", today);
            return;
        }

        // Acquire distributed lock
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                LOCK_KEY, "locked", Duration.ofSeconds(LOCK_TTL_SECONDS));
        if (Boolean.FALSE.equals(locked)) {
            log.debug("Reminder generation already running on another instance, skipping");
            return;
        }

        LocalDateTime startTime = LocalDateTime.now();
        try {
            int taskCount = reminderTaskService.generateTasks(today);
            log.info("Reminder generation completed for {}: {} tasks generated, duration={}ms",
                    today, taskCount,
                    Duration.between(startTime, LocalDateTime.now()).toMillis());
        } catch (Exception e) {
            log.error("Reminder generation failed for date {}", today, e);
            // Delete idempotency key so task retries next day (per WR-04 fix pattern)
            redisTemplate.delete("reminder:gen:last-run:" + today);
        } finally {
            // Release lock
            redisTemplate.delete(LOCK_KEY);
        }
    }
}
