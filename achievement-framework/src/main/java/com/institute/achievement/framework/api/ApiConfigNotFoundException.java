package com.institute.achievement.framework.api;

/**
 * Exception thrown when an API configuration is not found or is inactive.
 * <p>
 * Used by ApiClientFactory to indicate that a requested config code
 * does not exist or is disabled (status=0).
 */
public class ApiConfigNotFoundException extends RuntimeException {

    /**
     * Create an exception for a missing config code.
     *
     * @param configCode the configuration code that was not found
     */
    public ApiConfigNotFoundException(String configCode) {
        super("API configuration not found or inactive: " + configCode);
    }
}
