package com.institute.achievement.framework.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * View object for API connection test results.
 * <p>
 * Returned by the test connection endpoint to provide detailed
 * feedback about the connectivity to the configured external API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestConnectionResultVO {

    /** Whether the connection test succeeded */
    private Boolean success;

    /** Result message ("连接成功" or "连接失败: {detail}") */
    private String message;

    /** Response time in milliseconds */
    private Long responseTimeMs;

    /** HTTP status code returned by the external service */
    private Integer statusCode;
}
