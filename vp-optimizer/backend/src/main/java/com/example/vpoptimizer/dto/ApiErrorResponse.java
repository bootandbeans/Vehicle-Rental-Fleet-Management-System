package com.example.vpoptimizer.dto;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error payload.
 *
 * <p>Never contains stack traces or internal class names - clients only see a stable {@code code},
 * a human readable {@code message} and optional per-field validation details.</p>
 */
public record ApiErrorResponse(String code,
                               String message,
                               Instant timestamp,
                               String path,
                               List<FieldErrorDetail> fieldErrors) {

    public record FieldErrorDetail(String field, String message) {
    }

    public static ApiErrorResponse of(String code, String message, String path) {
        return new ApiErrorResponse(code, message, Instant.now(), path, List.of());
    }

    public static ApiErrorResponse withFieldErrors(String code, String message, String path,
                                                   List<FieldErrorDetail> fieldErrors) {
        return new ApiErrorResponse(code, message, Instant.now(), path, List.copyOf(fieldErrors));
    }
}
