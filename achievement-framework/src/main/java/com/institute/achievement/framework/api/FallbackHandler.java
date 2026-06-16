package com.institute.achievement.framework.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Functional interface for handling API call failures with graceful degradation.
 * <p>
 * When an external API call fails after all retries, the fallback handler
 * provides an alternative response so the calling code can degrade gracefully
 * instead of throwing an exception (API-04).
 * <p>
 * Default implementations are provided for common fallback strategies:
 * <ul>
 *   <li>{@link #emptyResponse()} - returns success=false with null data</li>
 *   <li>{@link #defaultData(Object)} - returns success=false with default data</li>
 *   <li>{@link #fromCache(String, Class)} - returns cached data if available</li>
 * </ul>
 * All implementations log the exception before returning the fallback.
 *
 * @param <T> the response data type
 */
@FunctionalInterface
public interface FallbackHandler<T> {

    Logger log = LoggerFactory.getLogger(FallbackHandler.class);

    /**
     * Handle an API exception and return a fallback response.
     *
     * @param exception the exception from the failed API call
     * @return a fallback API response (never null)
     */
    ApiClient.ApiResponse<T> handle(ApiException exception);

    /**
     * Return a fallback handler that returns an empty response
     * with success=false and data=null.
     *
     * @param <T> the response data type
     * @return a fallback handler returning an empty response
     */
    static <T> FallbackHandler<T> emptyResponse() {
        return exception -> {
            log.warn("API call failed, returning empty fallback: {}", exception.getMessage());
            return ApiClient.ApiResponse.<T>builder()
                    .success(false)
                    .build();
        };
    }

    /**
     * Return a fallback handler that returns a default data value
     * with success=false.
     *
     * @param <T>         the response data type
     * @param defaultData the default data to return on failure
     * @return a fallback handler returning the default data
     */
    static <T> FallbackHandler<T> defaultData(T defaultData) {
        return exception -> {
            log.warn("API call failed, returning default data fallback: {}", exception.getMessage());
            return ApiClient.ApiResponse.<T>builder()
                    .success(false)
                    .data(defaultData)
                    .build();
        };
    }

    /**
     * Return a fallback handler that returns cached data if available.
     * <p>
     * Phase 0: cache integration deferred; always falls back to empty.
     * Phase 1+ will integrate with Redis cache for full implementation.
     *
     * @param <T>      the response data type
     * @param cacheKey the cache key to look up
     * @param type     the data type class
     * @return a fallback handler returning cached data, or empty on cache miss
     */
    static <T> FallbackHandler<T> fromCache(String cacheKey, Class<T> type) {
        return exception -> {
            log.warn("API call failed, checking cache '{}' for fallback: {}", cacheKey, exception.getMessage());
            // Phase 0: cache fallback returns empty; Phase 1+ will implement Redis lookup
            log.debug("Cache fallback for '{}' not available in Phase 0", cacheKey);
            return ApiClient.ApiResponse.<T>builder()
                    .success(false)
                    .build();
        };
    }
}
