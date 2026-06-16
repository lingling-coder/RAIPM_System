package com.institute.achievement.framework.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Interface for making retryable API calls to external services.
 * <p>
 * Provides methods for executing HTTP requests with Resilience4j retry
 * and optional fallback handling for graceful degradation per API-04.
 * <p>
 * Usage:
 * <pre>{@code
 * ApiClient client = apiClientFactory.createClient("crossref_api");
 * RequestSpec request = RequestSpec.builder()
 *     .method(HttpMethod.GET)
 *     .path("/works/10.1000/xyz")
 *     .build();
 * ApiResponse<Map> response = client.execute(request);
 * }</pre>
 */
public interface ApiClient {

    /**
     * Execute an API request with Resilience4j retry.
     *
     * @param <T>     the expected response data type
     * @param request the request specification (method, path, headers, etc.)
     * @return the API response with data payload
     * @throws ApiException if the API call fails after all retries
     */
    <T> ApiResponse<T> execute(RequestSpec request);

    /**
     * Execute an API request with retry and fallback.
     * If the API call fails after all retries, the fallback handler
     * provides a default response (graceful degradation per API-04).
     *
     * @param <T>      the expected response data type
     * @param request  the request specification
     * @param fallback the fallback handler for graceful degradation
     * @return the API response, or fallback result on failure
     */
    <T> ApiResponse<T> executeWithFallback(RequestSpec request, FallbackHandler<T> fallback);

    /**
     * Request specification for an API call.
     * <p>
     * Describes the HTTP method, relative path, headers, query parameters,
     * and request body for the external API call. The base endpoint URL
     * is already configured in the ApiClient instance.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class RequestSpec {
        /** HTTP method: GET, POST, PUT, DELETE */
        private String method;

        /** Relative path appended to the endpoint URL */
        private String path;

        /** HTTP request headers */
        private Map<String, String> headers;

        /** Query parameters */
        private Map<String, String> queryParams;

        /** Request body object (serialized to JSON) */
        private Object body;
    }

    /**
     * Typed API response wrapper.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ApiResponse<T> {
        /** Whether the API call succeeded */
        private boolean success;

        /** Response data payload */
        private T data;

        /** HTTP status code */
        private int statusCode;

        /** Response time in milliseconds */
        private long responseTimeMs;
    }
}
