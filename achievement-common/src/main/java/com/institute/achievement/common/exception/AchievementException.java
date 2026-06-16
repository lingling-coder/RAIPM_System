package com.institute.achievement.common.exception;

import lombok.Getter;

/**
 * Domain-specific exception for achievement business logic violations.
 * Carries an error code suitable for API responses and frontend display.
 *
 * Message format: "E{errorCode}: {description}"
 */
@Getter
public class AchievementException extends RuntimeException {

    private final int errorCode;

    public AchievementException(int errorCode, String message) {
        super("E" + errorCode + ": " + message);
        this.errorCode = errorCode;
    }

    public AchievementException(int errorCode, String message, Throwable cause) {
        super("E" + errorCode + ": " + message, cause);
        this.errorCode = errorCode;
    }

    // ── Common Error Codes ──────────────────────────────────────────

    /** Duplicate DOI / application number */
    public static final int DUPLICATE_ACHIEVEMENT = 4001;

    /** Invalid status transition */
    public static final int INVALID_STATUS_TRANSITION = 4002;

    /** Required field missing */
    public static final int MISSING_REQUIRED_FIELD = 4003;

    /** Not authorized for this action */
    public static final int NOT_AUTHORIZED = 4004;

    /** Entity not found */
    public static final int NOT_FOUND = 4005;

    /** File exceeds size limit */
    public static final int FILE_SIZE_EXCEEDED = 4006;

    /** Unsupported file type */
    public static final int UNSUPPORTED_FILE_TYPE = 4007;

    /** DOI lookup failed */
    public static final int DOI_LOOKUP_FAILED = 4008;

    // ── Factory Methods ─────────────────────────────────────────────

    public static AchievementException duplicate(String field, String value) {
        return new AchievementException(DUPLICATE_ACHIEVEMENT,
                "该" + field + "已存在: " + value);
    }

    public static AchievementException invalidTransition(String current, String target) {
        return new AchievementException(INVALID_STATUS_TRANSITION,
                "当前状态(" + current + ")不允许此操作");
    }

    public static AchievementException notFound(String type, Long id) {
        return new AchievementException(NOT_FOUND,
                "未找到" + type + ": id=" + id);
    }

    public static AchievementException notAuthorized(String message) {
        return new AchievementException(NOT_AUTHORIZED, message);
    }
}
