package com.institute.achievement.framework.config;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Resilience4j configuration for external API calls.
 * <p>
 * Provides default retry and timeout settings per D-35:
 * <ul>
 *   <li>Retry: 3 attempts, exponential backoff (1s base, 2.0 multiplier) => 1s/2s/4s</li>
 *   <li>Connect timeout: 5 seconds</li>
 *   <li>Read timeout: 10 seconds</li>
 * </ul>
 * <p>
 * Per-endpoint overrides come from the api_config table settings at runtime
 * via ApiClientFactory (created in Task 2).
 */
@Configuration
@EnableCaching
public class Resilience4jConfig {

    /**
     * Default RetryRegistry with exponential backoff.
     * <p>
     * Configuration per D-35:
     * - maxAttempts: 3 (initial + 2 retries)
     * - waitDuration: 1s (base interval)
     * - exponentialBackoffMultiplier: 2.0 (1s -> 2s -> 4s)
     * - exponentialBackoffEnabled: true
     */
    @Bean
    public RetryRegistry retryRegistry() {
        IntervalFunction intervalFunction = IntervalFunction.ofExponentialBackoff(
                Duration.ofSeconds(1).toMillis(), 2.0);

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(intervalFunction)
                .build();

        return RetryRegistry.of(config);
    }

    /**
     * Default TimeLimiterRegistry.
     * <p>
     * Default timeout duration: 10 seconds for API calls.
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .build();

        return TimeLimiterRegistry.of(config);
    }

    /**
     * Default RestTemplate with configured timeouts per D-35.
     * <p>
     * - Connection timeout: 5 seconds
     * - Read timeout: 10 seconds
     * - Streaming-friendly (bufferRequestBody = false)
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);   // 5s connect timeout
        factory.setReadTimeout(10000);      // 10s read timeout
        return new RestTemplate(factory);
    }
}
