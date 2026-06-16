package com.institute.achievement.common.enums;

/**
 * Standard API result codes used across the system.
 */
public enum ResultCode {

    SUCCESS(200, "Success"),
    CREATED(201, "Created"),
    NO_CONTENT(204, "No Content"),

    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    CONFLICT(409, "Conflict"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),

    INTERNAL_ERROR(500, "Internal Server Error"),
    NOT_IMPLEMENTED(501, "Not Implemented"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),

    LOCKED(423, "Locked"),
    TOO_MANY_REQUESTS(429, "Too Many Requests"),

    // Business-specific codes (4xxx range for validation)
    VALIDATION_ERROR(4000, "Validation Error"),
    BUSINESS_ERROR(4001, "Business Error"),
    DATA_EXISTS(4002, "Data Already Exists"),
    DATA_NOT_EXISTS(4003, "Data Not Found"),
    OPERATION_NOT_ALLOWED(4004, "Operation Not Allowed");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
