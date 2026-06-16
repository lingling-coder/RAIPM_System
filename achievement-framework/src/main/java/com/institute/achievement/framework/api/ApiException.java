package com.institute.achievement.framework.api;

/**
 * Exception thrown when an external API call fails.
 * <p>
 * Wraps the underlying HTTP error or connection failure
 * so that callers can handle it via FallbackHandler
 * or propagate it as appropriate.
 */
public class ApiException extends RuntimeException {

    /** HTTP status code (if applicable) */
    private final Integer statusCode;

    /**
     * Create an API exception with a message.
     *
     * @param message the error description
     */
    public ApiException(String message) {
        super(message);
        this.statusCode = null;
    }

    /**
     * Create an API exception wrapping a cause.
     *
     * @param message the error description
     * @param cause   the underlying cause (e.g., connection timeout)
     */
    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    /**
     * Create an API exception with HTTP status code.
     *
     * @param message    the error description
     * @param statusCode the HTTP status code
     */
    public ApiException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Get the HTTP status code, if available.
     *
     * @return status code, or null if not applicable
     */
    public Integer getStatusCode() {
        return statusCode;
    }
}
