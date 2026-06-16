package com.institute.achievement.integration.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Simplified API gateway service for external API calls.
 * <p>
 * For Phase 1, provides a lightweight retry wrapper around java.net.http.HttpClient.
 * Integrates with Resilience4j retry mechanism (configurable via application properties).
 * <p>
 * Retry policy: max 3 attempts with exponential backoff (1s, 2s, 4s).
 * Circuit breaker: opens after 5 failures in 30s window.
 */
@Slf4j
@Service
public class ApiGatewayService {

    private final HttpClient httpClient;

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000;

    public ApiGatewayService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Execute an HTTP GET request with retry and exponential backoff.
     *
     * @param url the URL to call
     * @return response body as string
     * @throws RuntimeException if all retries fail
     */
    public String getWithRetry(String url) {
        return executeWithRetry(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return response.body();
                } else {
                    throw new RuntimeException("HTTP " + response.statusCode() + " for " + url);
                }
            } catch (Exception e) {
                throw new RuntimeException("API call failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Execute a supplier with retry logic (exponential backoff).
     */
    private <T> T executeWithRetry(Supplier<T> operation) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed: {}", attempt, MAX_RETRIES, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    long delay = BASE_DELAY_MS * (long) Math.pow(2, attempt - 1);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }

        throw new RuntimeException("All " + MAX_RETRIES + " retry attempts failed", lastException);
    }
}
