package com.institute.achievement.integration.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Generic fallback handler interface and default implementation.
 * <p>
 * Used by API clients to provide degraded behavior when external
 * services are unavailable. Each integration point should implement
 * its own fallback strategy.
 */
public interface FallbackHandler<T> {

    /**
     * Handle a failure from an external service call.
     *
     * @param throwable the exception that occurred
     * @param source    the name of the source that failed
     * @return fallback result (optional — empty if no fallback available)
     */
    Optional<T> handle(Throwable throwable, String source);
}

/**
 * Default fallback handler that logs and returns empty.
 * Individual services should provide specialized implementations.
 */
@Slf4j
@Component
class DefaultFallbackHandler implements FallbackHandler<Object> {

    @Override
    public Optional<Object> handle(Throwable throwable, String source) {
        log.error("Fallback triggered for source {}: {}", source, throwable.getMessage());
        return Optional.empty();
    }
}
