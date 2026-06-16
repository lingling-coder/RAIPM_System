package com.institute.achievement.common.exception;

import com.institute.achievement.common.enums.ResultCode;

/**
 * Exception thrown when a requested entity is not found.
 */
public class EntityNotFoundException extends BaseException {

    public EntityNotFoundException(String message) {
        super(ResultCode.DATA_NOT_EXISTS.getCode(), message);
    }

    public EntityNotFoundException(String entityName, Long id) {
        super(ResultCode.DATA_NOT_EXISTS.getCode(), entityName + " not found: " + id);
    }
}
