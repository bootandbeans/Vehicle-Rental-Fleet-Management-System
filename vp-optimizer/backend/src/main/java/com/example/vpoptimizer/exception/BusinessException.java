package com.example.vpoptimizer.exception;

/**
 * Base type for all expected, business level failures.
 *
 * <p>Carries an {@link ApiErrorCode} so the REST layer can translate the failure into a stable
 * error payload without leaking implementation details (or stack traces) to clients.</p>
 */
public class BusinessException extends RuntimeException {

    private final ApiErrorCode code;

    public BusinessException(ApiErrorCode code) {
        this(code, code.defaultMessage());
    }

    public BusinessException(ApiErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ApiErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ApiErrorCode code() {
        return code;
    }
}
