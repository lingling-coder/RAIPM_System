package com.institute.achievement.common.exception;

/**
 * Abstract base exception for all business exceptions in the achievement management system.
 * All custom business exceptions should extend this class.
 */
public abstract class BaseException extends RuntimeException {

    private final Integer code;
    private final String message;
    private final Object[] args;

    protected BaseException(Integer code, String message, Object... args) {
        super(message);
        this.code = code;
        this.message = message;
        this.args = args;
    }

    protected BaseException(Integer code, String message, Throwable cause, Object... args) {
        super(message, cause);
        this.code = code;
        this.message = message;
        this.args = args;
    }

    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public Object[] getArgs() {
        return args;
    }
}
