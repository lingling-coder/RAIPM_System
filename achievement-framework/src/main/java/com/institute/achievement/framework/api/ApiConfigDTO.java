package com.institute.achievement.framework.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for API configuration.
 * <p>
 * Used for create/update requests and response serialization.
 * Sensitive fields (apiKey, secretKey) are automatically masked
 * in response contexts via {@link #maskSensitiveData()}.
 */
@Data
public class ApiConfigDTO {

    private Long id;

    /** Configuration display name (required) */
    @NotBlank(message = "配置名称不能为空")
    private String configName;

    /** Unique configuration code (required) */
    @NotBlank(message = "配置编码不能为空")
    private String configCode;

    /** Base endpoint URL (required) */
    @NotBlank(message = "接口地址不能为空")
    private String endpointUrl;

    /** Description of the configuration */
    private String description;

    /** Authentication type: NONE, API_KEY, BEARER, BASIC */
    private String authType;

    /** API key (masked in responses) */
    private String apiKey;

    /** Secret key (masked in responses) */
    private String secretKey;

    /** OAuth2 token URL */
    private String tokenUrl;

    /** Connection timeout in seconds */
    @Min(value = 1, message = "连接超时必须大于0")
    private Integer connectTimeout;

    /** Read timeout in seconds */
    @Min(value = 1, message = "读取超时必须大于0")
    private Integer readTimeout;

    /** Number of retry attempts */
    @Min(value = 0, message = "重试次数不能小于0")
    private Integer retryCount;

    /** Base retry interval in seconds */
    @Min(value = 1, message = "重试间隔必须大于0")
    private Integer retryInterval;

    /** Backoff strategy: EXPONENTIAL or FIXED */
    private String backoffStrategy;

    /** Failure alert toggle */
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

    /**
     * Mask sensitive fields (apiKey, secretKey) for response display.
     * Replaces the middle portion of the value with "****",
     * showing only the first 4 and last 4 characters.
     * <p>
     * If the value is null or shorter than 9 characters,
     * it is replaced entirely with "****".
     */
    public void maskSensitiveData() {
        this.apiKey = maskValue(this.apiKey);
        this.secretKey = maskValue(this.secretKey);
    }

    /**
     * Mask a sensitive string value.
     * Shows first 4 chars + "****" + last 4 chars for values >= 9 chars.
     * Shows "****" for null or short values.
     */
    private String maskValue(String value) {
        if (value == null) {
            return null;
        }
        int len = value.length();
        if (len < 9) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(len - 4);
    }
}
