package com.institute.achievement.framework.api;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.institute.achievement.framework.api.impl.RetryableApiClientImpl;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating API client instances with per-configuration settings.
 * <p>
 * Creates {@link RetryableApiClientImpl} instances configured with:
 * <ul>
 *   <li>Per-endpoint timeouts (connectTimeout, readTimeout from api_config)</li>
 *   <li>Per-endpoint retry settings (retryCount, retryInterval, backoffStrategy)</li>
 *   <li>Authentication headers based on auth type</li>
 * </ul>
 * <p>
 * Clients are cached by configCode for reuse. The cache is evicted when
 * the configuration is updated or deleted (runtime config reload per D-34).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiClientFactory {

    private final ApiConfigService apiConfigService;
    private final RetryRegistry retryRegistry;

    /** Cache of created API clients by config code */
    private final ConcurrentHashMap<String, ApiClient> clientCache = new ConcurrentHashMap<>();

    /**
     * Create (or retrieve from cache) an API client for the given config code.
     * <p>
     * The client is configured with the endpoint's specific timeout and retry settings
     * from the api_config table. If the configuration is not found or inactive,
     * an {@link ApiConfigNotFoundException} is thrown.
     *
     * @param configCode the configuration code (e.g., "crossref_api")
     * @return a configured API client instance
     * @throws ApiConfigNotFoundException if the config code is not found or inactive
     */
    public ApiClient createClient(String configCode) {
        return clientCache.computeIfAbsent(configCode, this::buildClient);
    }

    /**
     * Evict a cached client by config code.
     * Called when the configuration is updated or deleted.
     *
     * @param configCode the configuration code to evict
     */
    public void evictClient(String configCode) {
        clientCache.remove(configCode);
        log.debug("Evicted API client cache for '{}'", configCode);
    }

    /**
     * Evict all cached clients.
     * Called when multiple configurations change simultaneously.
     */
    public void evictAll() {
        clientCache.clear();
        log.debug("Evicted all API client caches");
    }

    /**
     * Build a new API client for the given config code.
     */
    private ApiClient buildClient(String configCode) {
        // Load configuration from DB (unmasked, for programmatic use)
        ApiConfigEntity config = apiConfigService.getActiveByCode(configCode);
        if (config == null) {
            throw new ApiConfigNotFoundException(configCode);
        }

        // Create per-config RestTemplate with custom timeouts
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getConnectTimeout() * 1000);
        factory.setReadTimeout(config.getReadTimeout() * 1000);
        RestTemplate clientRestTemplate = new RestTemplate(factory);

        // Create per-config Retry with endpoint-specific settings
        Retry retry = buildRetry(config, configCode);

        log.info("Created API client for '{}' -> {} (timeout: {}s/{}s, retry: {})",
                configCode, config.getEndpointUrl(),
                config.getConnectTimeout(), config.getReadTimeout(),
                config.getRetryCount());

        return new RetryableApiClientImpl(clientRestTemplate, retry, config);
    }

    /**
     * Build a Resilience4j Retry object from configuration settings.
     */
    private Retry buildRetry(ApiConfigEntity config, String configCode) {
        int retryCount = config.getRetryCount() != null ? config.getRetryCount() : 3;
        int retryInterval = config.getRetryInterval() != null ? config.getRetryInterval() : 1;
        String backoffStrategy = config.getBackoffStrategy() != null ? config.getBackoffStrategy() : "EXPONENTIAL";

        RetryConfig.Builder<Object> builder = RetryConfig.custom()
                .maxAttempts(retryCount + 1);  // +1 because maxAttempts includes the initial call

        if ("FIXED".equalsIgnoreCase(backoffStrategy)) {
            builder.waitDuration(Duration.ofSeconds(retryInterval));
        } else {
            // EXPONENTIAL: base interval * multiplier^n
            IntervalFunction intervalFunction = IntervalFunction.ofExponentialBackoff(
                    Duration.ofSeconds(retryInterval).toMillis(), 2.0);
            builder.intervalFunction(intervalFunction);
        }

        RetryConfig retryConfig = builder.build();
        return retryRegistry.retry(configCode, retryConfig);
    }
}
