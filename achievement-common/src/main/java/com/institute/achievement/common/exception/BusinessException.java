package com.institute.achievement.common.exception;

import com.institute.achievement.common.enums.ResultCode;

/**
 * Business logic exception. Thrown when a business rule is violated.
 */
public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(ResultCode.BUSINESS_ERROR.getCode(), message);
    }

    public BusinessException(String message, Object... args) {
        super(ResultCode.BUSINESS_ERROR.getCode(), message, args);
    }

    public BusinessException(String message, Throwable cause) {
        super(ResultCode.BUSINESS_ERROR.getCode(), message, cause);
    }
}
