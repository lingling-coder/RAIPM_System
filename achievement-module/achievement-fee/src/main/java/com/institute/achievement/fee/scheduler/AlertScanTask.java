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
 * Daily scheduled task for 4-tier fee alert generation.
 * <p>
 * Runs daily at 4 AM (after fee plan generation at 3 AM), scans all pending
 * fee records and classifies them into BLUE/YELLOW/ORANGE/RED alert levels
 * based on due date proximity. Persists alert records and sends in-app
 * notifications to the responsible users.
 * <p>
 * Uses Redis distributed lock (matching FeePlanGenerationTask pattern) plus
 * an idempotency check to prevent duplicate execution on restart.
 *
 * <h3>Threat model alignment</h3>
 * <ul>
 *   <li>T-02-03-01: UNIQUE INDEX + batch dedup prevents duplicate alert insert</li>
 *   <li>T-02-03-02: Redis idempotency key + distributed lock prevents concurrent/duplicate execution</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertScanTask {

    private static final String LOCK_KEY = "spring:lock:alert-scan";
    private static final long LOCK_TTL_SECONDS = 300;

    private final StringRedisTemplate redisTemplate;
    private final AlertRecordService alertRecordService;

    /**
     * Daily alert scan at 4 AM.
     * <p>
     * Idempotency: checks Redis key "fee:alert:last-run:YYYY-MM-DD" before executing.
     * Only proceeds if no record of today's run exists.
     * Lock TTL: 5 minutes (scan should complete in < 1 minute for < 10K fee records).
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void scanAlerts() {
        String today = LocalDate.now().toString();

        // Idempotency check: skip if already run today
        Boolean alreadyRun = redisTemplate.opsForValue().setIfAbsent(
                "fee:alert:last-run:" + today, "running",
                Duration.ofHours(2));
        if (Boolean.FALSE.equals(alreadyRun)) {
            log.debug("Alert scan already executed for date {}, skipping", today);
            return;
        }

        // Acquire distributed lock
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                LOCK_KEY, "locked", Duration.ofSeconds(LOCK_TTL_SECONDS));
        if (Boolean.FALSE.equals(locked)) {
            log.debug("Alert scan already running on another instance, skipping");
            return;
        }

        LocalDateTime startTime = LocalDateTime.now();
        try {
            int alertCount = alertRecordService.scanAndGenerateAlerts();
            log.info("Alert scan completed for {}: {} alerts generated, duration={}ms",
                    today, alertCount,
                    Duration.between(startTime, LocalDateTime.now()).toMillis());
        } catch (Exception e) {
            log.error("Alert scan failed for date {}", today, e);
            // Remove idempotency key so the task can be retried (WR-04)
            redisTemplate.delete("fee:alert:last-run:" + today);
        } finally {
            // Release lock
            redisTemplate.delete(LOCK_KEY);
        }
    }
}
