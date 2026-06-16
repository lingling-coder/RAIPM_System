package com.institute.achievement.fee.scheduler;

import com.institute.achievement.fee.service.AlertRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Daily scheduled task for 3-tier alert escalation (D-24).
 * <p>
 * Runs daily at 5 AM (after the alert scan at 4 AM), scans all pending
 * alert records and escalates unresolved ones based on age:
 * <ol>
 *   <li>&ge; 3 days unresolved -> escalate to department head (ROLE_SECRETARY / ROLE_DEPT_ADMIN)</li>
 *   <li>&ge; 8 days unresolved -> escalate to leadership (ROLE_LEADER)</li>
 * </ol>
 * <p>
 * Uses Redis distributed lock (matching AlertScanTask pattern) plus
 * an idempotency check to prevent duplicate execution on restart.
 *
 * <h3>Threat model alignment</h3>
 * <ul>
 *   <li>T-02-04-03: RBAC query uses sys_user_role + sys_role JOIN with exact role_code.
 *       DEPT_HEAD query includes dept_id filter. LEADERSHIP query uses role_code='ROLE_LEADER'.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEscalationTask {

    private static final String LOCK_KEY = "spring:lock:alert-escalation";
    private static final long LOCK_TTL_SECONDS = 300;

    private final StringRedisTemplate redisTemplate;
    private final AlertRecordService alertRecordService;

    /**
     * Daily escalation scan at 5 AM.
     * <p>
     * Idempotency: checks Redis key "fee:alert:escalation:last-run:YYYY-MM-DD" before executing.
     * Only proceeds if no record of today's run exists.
     * Lock TTL: 5 minutes (scan should complete in < 1 minute for < 10K alert records).
     */
    @Scheduled(cron = "0 0 5 * * ?")
    public void processEscalations() {
        String today = LocalDate.now().toString();

        // Idempotency check: skip if already run today
        Boolean alreadyRun = redisTemplate.opsForValue().setIfAbsent(
                "fee:alert:escalation:last-run:" + today, "running",
                Duration.ofHours(2));
        if (Boolean.FALSE.equals(alreadyRun)) {
            log.debug("Alert escalation already executed for date {}, skipping", today);
            return;
        }

        // Acquire distributed lock
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                LOCK_KEY, "locked", Duration.ofSeconds(LOCK_TTL_SECONDS));
        if (Boolean.FALSE.equals(locked)) {
            log.debug("Alert escalation already running on another instance, skipping");
            return;
        }

        LocalDateTime startTime = LocalDateTime.now();
        try {
            int escalatedCount = alertRecordService.processEscalations();
            log.info("Alert escalation completed for {}: {} alerts escalated, duration={}ms",
                    today, escalatedCount,
                    Duration.between(startTime, LocalDateTime.now()).toMillis());
        } catch (Exception e) {
            log.error("Alert escalation failed for date {}", today, e);
            // Remove idempotency key so the task can be retried (WR-04)
            redisTemplate.delete("fee:alert:escalation:last-run:" + today);
        } finally {
            // Release lock
            redisTemplate.delete(LOCK_KEY);
        }
    }
}
