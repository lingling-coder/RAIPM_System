package com.institute.achievement.common.exception;

import com.institute.achievement.common.enums.ResultCode;

/**
 * Exception thrown for invalid request parameters or validation failures.
 */
public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super(ResultCode.VALIDATION_ERROR.getCode(), message);
    }

    public BadRequestException(String message, Object... args) {
        super(ResultCode.VALIDATION_ERROR.getCode(), message, args);
    }
}
