package com.institute.achievement.common.constant;

/**
 * API constants including HTTP header names, pagination defaults, and common API values.
 */
public final class ApiConstants {

    private ApiConstants() {
        // prevent instantiation
    }

    // ── HTTP Headers ──────────────────────────────────────────────────────────

    /** Authorization header name */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /** Bearer token prefix */
    public static final String BEARER_PREFIX = "Bearer ";

    /** Refresh token header */
    public static final String HEADER_REFRESH_TOKEN = "Refresh-Token";

    /** Request ID header for tracing */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    /** Tenant/Department header */
    public static final String HEADER_DEPT_ID = "X-Dept-Id";

    // ── Pagination Defaults ───────────────────────────────────────────────────

    /** Default page number (1-indexed) */
    public static final int DEFAULT_PAGE = 1;

    /** Default page size */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** Maximum page size to prevent abuse */
    public static final int MAX_PAGE_SIZE = 500;

    // ── API Path Prefixes ─────────────────────────────────────────────────────

    /** API prefix for version 1 */
    public static final String API_V1_PREFIX = "/api/v1";

    /** System management API prefix */
    public static final String API_SYSTEM_PREFIX = API_V1_PREFIX + "/system";

    // ── Charset ───────────────────────────────────────────────────────────────

    public static final String CHARSET_UTF_8 = "UTF-8";

    // ── Date/Time Formats ─────────────────────────────────────────────────────

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
}
