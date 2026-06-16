package com.institute.achievement.common.util;

import com.institute.achievement.common.enums.ResultCode;
import lombok.Data;

import java.io.Serializable;

/**
 * Standard API response wrapper.
 * All REST controllers return this format.
 *
 * @param <T> the type of the data payload
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Status code, 200 = success */
    private int code;

    /** Response message */
    private String message;

    /** Response payload */
    private T data;

    private Result() {
    }

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ── Static Factory Methods ───────────────────────────────

    /**
     * Success with data and default message.
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * Success with no data (for void operations).
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * Success with custom message and data.
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * Created response (201).
     */
    public static <T> Result<T> created(T data) {
        return new Result<>(ResultCode.CREATED.getCode(), ResultCode.CREATED.getMessage(), data);
    }

    /**
     * Error response with specific result code and message.
     */
    public static <T> Result<T> error(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

    /**
     * Error response with custom code and message.
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    // ── Convenience Error Methods ────────────────────────────

    public static <T> Result<T> badRequest(String message) {
        return error(ResultCode.BAD_REQUEST, message);
    }

    public static <T> Result<T> notFound(String message) {
        return error(ResultCode.NOT_FOUND, message);
    }

    public static <T> Result<T> forbidden(String message) {
        return error(ResultCode.FORBIDDEN, message);
    }

    public static <T> Result<T> unauthorized(String message) {
        return error(ResultCode.UNAUTHORIZED, message);
    }

    public static <T> Result<T> conflict(String message) {
        return error(ResultCode.CONFLICT, message);
    }

    public static <T> Result<T> internalError(String message) {
        return error(ResultCode.INTERNAL_ERROR, message);
    }

    /**
     * Check if this response indicates success.
     */
    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }
}
