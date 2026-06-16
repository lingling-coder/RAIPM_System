package com.institute.achievement.framework.api.impl;

import com.institute.achievement.framework.api.ApiClient;
import com.institute.achievement.framework.api.ApiConfigEntity;
import com.institute.achievement.framework.api.ApiException;
import com.institute.achievement.framework.api.FallbackHandler;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Implementation of the {@link ApiClient} interface with Resilience4j retry support.
 * <p>
 * Wraps HTTP calls to external APIs with:
 * <ul>
 *   <li>Per-configuration endpoint URL and timeouts</li>
 *   <li>Resilience4j retry with configurable attempts and backoff</li>
 *   <li>Authentication header injection (API_KEY, BEARER, BASIC, NONE)</li>
 *   <li>Response time measurement</li>
 *   <li>Optional fallback handling for graceful degradation</li>
 * </ul>
 */
@Slf4j
public class RetryableApiClientImpl implements ApiClient {

    private final RestTemplate restTemplate;
    private final Retry retry;
    private final ApiConfigEntity config;

    /**
     * Create a new retryable API client.
     *
     * @param restTemplate the RestTemplate with per-config timeouts
     * @param retry        the Resilience4j Retry object with per-config settings
     * @param config       the API configuration entity
     */
    public RetryableApiClientImpl(RestTemplate restTemplate, Retry retry, ApiConfigEntity config) {
        this.restTemplate = restTemplate;
        this.retry = retry;
        this.config = config;
    }

    @Override
    public <T> ApiResponse<T> execute(RequestSpec request) {
        long startTime = System.currentTimeMillis();

        try {
            // Wrap the HTTP call with Resilience4j retry
            Supplier<ApiResponse<T>> retryableSupplier = Retry.decorateSupplier(retry, () -> {
                try {
                    return doHttpCall(request);
                } catch (Exception e) {
                    log.warn("API call attempt failed for '{}': {}", config.getConfigCode(), e.getMessage());
                    throw new ApiException("API call failed: " + e.getMessage(), e);
                }
            });

            ApiResponse<T> response = retryableSupplier.get();
            long elapsed = System.currentTimeMillis() - startTime;
            response.setResponseTimeMs(elapsed);
            return response;

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("API call failed after retries for '{}' ({}ms): {}",
                    config.getConfigCode(), elapsed, e.getMessage());

            throw new ApiException("API call failed after " + retry.getRetryConfig().getMaxAttempts()
                    + " attempts: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> ApiResponse<T> executeWithFallback(RequestSpec request, FallbackHandler<T> fallback) {
        try {
            return execute(request);
        } catch (ApiException e) {
            log.warn("API call failed for '{}', invoking fallback handler",
                    config.getConfigCode());
            return fallback.handle(e);
        }
    }

    /**
     * Execute the actual HTTP request.
     */
    @SuppressWarnings("unchecked")
    private <T> ApiResponse<T> doHttpCall(RequestSpec request) {
        // Build full URL
        String baseUrl = config.getEndpointUrl();
        if (baseUrl.endsWith("/") && request.getPath() != null && request.getPath().startsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String fullPath = request.getPath() != null ? request.getPath() : "";
        String url = baseUrl + fullPath;

        // Build URI with query parameters
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(url);
        if (request.getQueryParams() != null) {
            for (Map.Entry<String, String> entry : request.getQueryParams().entrySet()) {
                uriBuilder.queryParam(entry.getKey(), entry.getValue());
            }
        }
        URI uri = uriBuilder.build().toUri();

        // Build headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Achievement-System/1.0");

        // Merge request-specific headers
        if (request.getHeaders() != null) {
            request.getHeaders().forEach(headers::set);
        }

        // Apply auth headers from config
        applyAuthHeaders(headers);

        // Build request entity
        HttpEntity<?> requestEntity = new HttpEntity<>(request.getBody(), headers);

        // Determine HTTP method
        HttpMethod method = HttpMethod.GET;
        if (request.getMethod() != null) {
            method = HttpMethod.valueOf(request.getMethod().toUpperCase());
        }

        // Execute request
        ResponseEntity<String> response = restTemplate.exchange(uri, method, requestEntity, String.class);
        String responseBody = response.getBody();

        // Build response
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(response.getStatusCode().is2xxSuccessful());
        apiResponse.setStatusCode(response.getStatusCode().value());
        apiResponse.setData((T) responseBody);  // Raw string data; callers deserialize as needed
        apiResponse.setResponseTimeMs(0);  // Will be set by caller

        return apiResponse;
    }

    /**
     * Apply authentication headers from the configuration.
     */
    private void applyAuthHeaders(HttpHeaders headers) {
        String authType = config.getAuthType() != null ? config.getAuthType() : "NONE";

        switch (authType.toUpperCase()) {
            case "API_KEY":
                if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
                    headers.set("X-API-Key", config.getApiKey());
                }
                break;

            case "BEARER":
                if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
                    headers.setBearerAuth(config.getApiKey());
                }
                break;

            case "BASIC":
                if (config.getApiKey() != null && config.getSecretKey() != null) {
                    String credentials = config.getApiKey() + ":" + config.getSecretKey();
                    String encoded = Base64.getEncoder().encodeToString(
                            credentials.getBytes(StandardCharsets.UTF_8));
                    headers.set("Authorization", "Basic " + encoded);
                }
                break;

            case "NONE":
            default:
                // No authentication
                break;
        }
    }
}
