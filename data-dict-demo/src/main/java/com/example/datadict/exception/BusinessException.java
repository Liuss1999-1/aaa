package com.example.datadict.exception;

/**
 * Business exception with HTTP-friendly status codes.
 * Use factory methods for semantic clarity.
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(400, message);
    }

    public int getCode() { return code; }

    public static BusinessException notFound(String entity, Object identifier) {
        return new BusinessException(404, entity + " not found: " + identifier);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(409, message);
    }
}
