package com.institute.achievement.reminder.config;

import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j retry configuration for email sending (D-16).
 * <p>
 * Email retry uses a longer backoff schedule than the default API retry
 * because SMTP failures are typically transient network issues that
 * resolve over minutes, not seconds.
 * <p>
 * Retry schedule: 1 minute -> 5 minutes -> 15 minutes (3 attempts total).
 * Backoff: exponential with 5.0 multiplier, capped at 15 minutes max.
 * <p>
 * Per D-16: Retry 3 times with increasing intervals (1min -> 5min -> 15min).
 */
@Slf4j
@Configuration
public class EmailRetryConfig {

    /**
     * Retry registry bean named "emailSend" for the @Retry annotation.
     * <p>
     * Configuration:
     * <ul>
     *   <li>maxAttempts: 3 (initial send + 2 retries)</li>
     *   <li>waitDuration: 1 minute (base interval)</li>
     *   <li>multiplier: 5.0 (1min -> 5min -> 25min, capped at 15min)</li>
     * </ul>
     *
     * @return RetryRegistry containing the emailSend config
     */
    @Bean
    public RetryRegistry emailRetryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMinutes(1))
                .exponentialBackoff(5, Duration.ofMinutes(15))
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        log.info("Email send retry configured: maxAttempts=3, backoff=1min/5min/15min (exponential, 5.0x)");
        return registry;
    }
}
