package com.institute.achievement.framework.api;

import com.institute.achievement.common.util.PageQuery;
import com.institute.achievement.common.util.PageResult;

/**
 * Service interface for API configuration management.
 * <p>
 * Provides CRUD operations for the P-08 API Integration Config page,
 * connection testing, and runtime config lookup for the API client framework.
 * Config is DB-stored and runtime-reloadable per D-34.
 */
public interface ApiConfigService {

    /**
     * Paginated query of API configurations.
     * Sensitive fields are masked in the returned DTOs.
     *
     * @param query pagination parameters
     * @return paginated result with masked sensitive fields
     */
    PageResult<ApiConfigDTO> page(PageQuery query);

    /**
     * Get a single configuration by ID.
     * Sensitive fields are masked in the returned DTO.
     *
     * @param id configuration ID
     * @return configuration DTO with masked sensitive fields
     */
    ApiConfigDTO getById(Long id);

    /**
     * Create a new API configuration.
     * Validates that configCode is unique.
     *
     * @param dto configuration data
     */
    void create(ApiConfigDTO dto);

    /**
     * Update an existing API configuration.
     *
     * @param id  configuration ID
     * @param dto updated configuration data
     */
    void update(Long id, ApiConfigDTO dto);

    /**
     * Delete an API configuration by ID.
     *
     * @param id configuration ID
     */
    void delete(Long id);

    /**
     * Test connectivity to the configured endpoint.
     * Sends an HTTP request with the configured auth type and timeouts,
     * measures response time, and updates last_test_time/result.
     *
     * @param id configuration ID to test
     * @return test result with success status, message, and response time
     */
    TestConnectionResultVO testConnection(Long id);

    /**
     * Get an active configuration by its code.
     * Returns the full entity with unmasked sensitive fields
     * for programmatic use by the API client framework.
     *
     * @param code the configuration code
     * @return the active (status=1) configuration entity, or null if not found/disabled
     */
    ApiConfigEntity getActiveByCode(String code);

    /**
     * Refresh the Redis cache for API configurations.
     * Called after create/update/delete to ensure runtime config reload.
     * Next call to getActiveByCode will load from DB.
     */
    void refreshCache();
}
