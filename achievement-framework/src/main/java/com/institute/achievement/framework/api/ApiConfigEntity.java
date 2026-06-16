package com.institute.achievement.framework.api;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API configuration entity mapped to the api_config table.
 * <p>
 * Stores external API integration settings including endpoint URL,
 * authentication credentials, timeout/retry configuration, and
 * connection test results. Managed via the P-08 configuration page
 * and consumed at runtime by the API client framework.
 * <p>
 * Per D-34: Config is DB-stored and runtime-reloadable (no restart).
 * Per D-35: Default retry 3x, exponential backoff, connect 5s, read 10s.
 */
@Data
@TableName("api_config")
public class ApiConfigEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Configuration display name, e.g. "Crossref API" */
    private String configName;

    /** Unique configuration code, e.g. "crossref_api" */
    private String configCode;

    /** Base endpoint URL for the external API */
    private String endpointUrl;

    /** Description of what this configuration is for */
    private String description;

    /** Authentication type: NONE, API_KEY, BEARER, BASIC */
    private String authType;

    /** API key (encrypted at rest in production) */
    private String apiKey;

    /** Secret key (for BASIC auth or OAuth2 flows) */
    private String secretKey;

    /** OAuth2 token URL (used for token-based auth flows) */
    private String tokenUrl;

    /** Connection timeout in seconds (default 5) */
    private Integer connectTimeout;

    /** Read timeout in seconds (default 10) */
    private Integer readTimeout;

    /** Number of retry attempts (default 3) */
    private Integer retryCount;

    /** Base retry interval in seconds (default 1) */
    private Integer retryInterval;

    /** Backoff strategy: EXPONENTIAL or FIXED */
    private String backoffStrategy;

    /** Whether to alert on failure: 0=off, 1=on */
    private Integer failureAlert;

    /** 1=enabled, 0=disabled */
    private Integer status;

    /** Last connection test timestamp */
    private LocalDateTime lastTestTime;

    /** Last connection test result: 1=success, 0=failure */
    private Integer lastTestResult;

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
