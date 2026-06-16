package com.institute.achievement.fee.scheduler;

import com.institute.achievement.fee.service.FeePlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Daily scheduled task for recurring annual fee plan generation.
 * <p>
 * Runs daily at 3 AM, scans all authorized patents and generates
 * upcoming annual fee plans (and corresponding fee records) for the
 * next period based on each patent's authorizationDate (D-14, D-15).
 * <p>
 * Uses Redis distributed lock (matching Phase 0 pattern) plus an
 * idempotency check to prevent duplicate generation on restart.
 *
 * <h3>Threat model alignment</h3>
 * <ul>
 *   <li>T-02-02-02: countByPatentAndType() + UNIQUE INDEX prevents duplicate insert</li>
 *   <li>T-02-02-03: Task runs once daily. If it fails, next day retries automatically</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeePlanGenerationTask {

    private static final String LOCK_KEY = "spring:lock:fee-plan-gen";
    private static final long LOCK_TTL_SECONDS = 300;

    private final StringRedisTemplate redisTemplate;
    private final FeePlanService feePlanService;

    /**
     * Daily fee plan generation at 3 AM.
     * <p>
     * Idempotency: checks Redis key "fee:plan-gen:last-run:YYYYMMDD" before executing.
     * Only proceeds if no record of today's run exists.
     * Lock TTL: 5 minutes (generation should complete in < 1 minute for < 10K patents).
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void generateRecurringPlans() {
        String today = LocalDate.now().toString();

        // Idempotency check: skip if already run today
        Boolean alreadyRun = redisTemplate.opsForValue().setIfAbsent(
                "fee:plan-gen:last-run:" + today, "running",
                Duration.ofHours(2));
        if (Boolean.FALSE.equals(alreadyRun)) {
            log.debug("Fee plan generation already executed for date {}, skipping", today);
            return;
        }

        // Acquire distributed lock
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                LOCK_KEY, "locked", Duration.ofSeconds(LOCK_TTL_SECONDS));
        if (Boolean.FALSE.equals(locked)) {
            log.debug("Fee plan generation already running on another instance, skipping");
            return;
        }

        LocalDateTime startTime = LocalDateTime.now();
        try {
            int createdCount = feePlanService.generateRecurringPlans();
            log.info("Fee plan generation completed: {} plans created, duration={}ms",
                    createdCount,
                    Duration.between(startTime, LocalDateTime.now()).toMillis());
        } catch (Exception e) {
            log.error("Fee plan generation failed", e);
            // Task will retry next day — partial failures are acceptable (D-16)
        } finally {
            // Release lock
            redisTemplate.delete(LOCK_KEY);
        }
    }
}
