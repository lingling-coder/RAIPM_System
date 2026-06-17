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
 * Daily scheduled task for deadline-relative reminder escalation (D-18, D-19).
 * <p>
 * Runs daily at 5 AM (after reminder generation at 3 AM), scans all unconfirmed
 * reminder tasks and escalates them based on deadline proximity and urgency level:
 * <ol>
 *   <li>HIGH urgency (&le; 7 days until deadline) &rarr; escalate NONE &rarr; DEPT_HEAD</li>
 *   <li>MEDIUM urgency (&le; 3 days until deadline) &rarr; escalate NONE &rarr; DEPT_HEAD</li>
 *   <li>LOW urgency: never escalates</li>
 *   <li>DEPT_HEAD &rarr; LEADERSHIP after 5+ days without confirmation</li>
 * </ol>
 * <p>
 * Uses Redis distributed lock (matching ReminderGenerationTask pattern) plus
 * an idempotency check to prevent duplicate execution on restart.
 *
 * <h3>Threat model alignment</h3>
 * <ul>
 *   <li>T-4-10: Redis distributed lock prevents concurrent execution; idempotency key
 *       prevents duplicate runs on restart</li>
 *   <li>T-4-11: Role-based routing uses exact role_code constants (ROLE_SECRETARY, ROLE_LEADER)
 *       with findUserIdsByDeptAndRole — matches Phase 2 pattern</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderEscalationTask {

    private static final String LOCK_KEY = "spring:lock:reminder-escalation";
    private static final long LOCK_TTL_SECONDS = 300;

    private final StringRedisTemplate redisTemplate;
    private final ReminderTaskService reminderTaskService;

    /**
     * Daily escalation scan at 5 AM.
     * <p>
     * Idempotency: checks Redis key {@code "reminder:escalation:last-run:YYYY-MM-DD"} before executing.
     * Only proceeds if no record of today's run exists.
     * Lock TTL: 5 minutes (scan should complete in &lt; 1 minute for typical task counts).
     * <p>
     * On failure the idempotency key is deleted so the task retries the next day.
     * The distributed lock is always released in the {@code finally} block.
     */
    @Scheduled(cron = "0 0 5 * * ?")
    public void processEscalations() {
        String today = LocalDate.now().toString();

        // Idempotency check: skip if already run today
        Boolean alreadyRun = redisTemplate.opsForValue().setIfAbsent(
                "reminder:escalation:last-run:" + today, "running",
                Duration.ofHours(2));
        if (Boolean.FALSE.equals(alreadyRun)) {
            log.debug("Reminder escalation already executed for date {}, skipping", today);
            return;
        }

        // Acquire distributed lock
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                LOCK_KEY, "locked", Duration.ofSeconds(LOCK_TTL_SECONDS));
        if (Boolean.FALSE.equals(locked)) {
            log.debug("Reminder escalation already running on another instance, skipping");
            return;
        }

        LocalDateTime startTime = LocalDateTime.now();
        try {
            int escalatedCount = reminderTaskService.processEscalations();
            log.info("Reminder escalation completed for {}: {} tasks escalated, duration={}ms",
                    today, escalatedCount,
                    Duration.between(startTime, LocalDateTime.now()).toMillis());
        } catch (Exception e) {
            log.error("Reminder escalation failed for date {}", today, e);
            // Delete idempotency key so the task can be retried next day
            redisTemplate.delete("reminder:escalation:last-run:" + today);
        } finally {
            // Release lock
            redisTemplate.delete(LOCK_KEY);
        }
    }
}
