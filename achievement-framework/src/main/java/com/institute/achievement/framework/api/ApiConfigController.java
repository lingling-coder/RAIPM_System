package com.institute.achievement.framework.api;

import com.institute.achievement.common.util.PageQuery;
import com.institute.achievement.common.util.PageResult;
import com.institute.achievement.common.util.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for API Integration Configuration (P-08).
 * <p>
 * Manages external API integration settings including CRUD operations,
 * paginated listing, and connection testing. All endpoints require
 * ROLE_SYSTEM_ADMIN access per P-10 RBAC rules.
 */
@RestController
@RequestMapping("/api/system/api-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_SYSTEM_ADMIN')")
public class ApiConfigController {

    private final ApiConfigService apiConfigService;

    /**
     * Paginated list of API configurations.
     * Sensitive fields (apiKey, secretKey) are masked in responses.
     */
    @PostMapping("/page")
    public Result<PageResult<ApiConfigDTO>> page(@RequestBody PageQuery query) {
        return Result.success(apiConfigService.page(query));
    }

    /**
     * Get a single API configuration by ID.
     * Sensitive fields are masked in the response.
     */
    @GetMapping("/{id}")
    public Result<ApiConfigDTO> getById(@PathVariable Long id) {
        return Result.success(apiConfigService.getById(id));
    }

    /**
     * Create a new API configuration.
     * Validates that configCode is unique.
     */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ApiConfigDTO dto) {
        apiConfigService.create(dto);
        return Result.success();
    }

    /**
     * Update an existing API configuration.
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ApiConfigDTO dto) {
        apiConfigService.update(id, dto);
        return Result.success();
    }

    /**
     * Delete an API configuration by ID.
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        apiConfigService.delete(id);
        return Result.success();
    }

    /**
     * Test connectivity to the configured API endpoint.
     * Sends an HTTP request with the configured auth and timeouts.
     */
    @PostMapping("/{id}/test")
    public Result<TestConnectionResultVO> testConnection(@PathVariable Long id) {
        return Result.success(apiConfigService.testConnection(id));
    }
}
