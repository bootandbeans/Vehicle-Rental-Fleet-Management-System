package com.example.vpoptimizer.exception;

import com.example.vpoptimizer.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Central translation of exceptions into stable API errors.
 *
 * <p>Unexpected exceptions are logged with their stack trace on the server and returned to the
 * client as a generic {@code INTERNAL_ERROR} payload - internals never leak to the frontend.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException exception,
                                                                   HttpServletRequest request) {
        ApiErrorCode code = exception.code();
        log.debug("Business failure {} on {}: {}", code, request.getRequestURI(), exception.getMessage());
        return ResponseEntity.status(code.status())
                .body(ApiErrorResponse.of(code.name(), exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBeanValidation(MethodArgumentNotValidException exception,
                                                                HttpServletRequest request) {
        List<ApiErrorResponse.FieldErrorDetail> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldError)
                .toList();
        String message = fieldErrors.isEmpty()
                ? ApiErrorCode.VALIDATION_FAILED.defaultMessage()
                : fieldErrors.get(0).message();
        return ResponseEntity.badRequest().body(ApiErrorResponse.withFieldErrors(
                ApiErrorCode.VALIDATION_FAILED.name(), message, request.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception,
                                                                     HttpServletRequest request) {
        List<ApiErrorResponse.FieldErrorDetail> fieldErrors = exception.getConstraintViolations().stream()
                .map(violation -> new ApiErrorResponse.FieldErrorDetail(lastNode(violation), violation.getMessage()))
                .toList();
        String message = fieldErrors.isEmpty()
                ? ApiErrorCode.VALIDATION_FAILED.defaultMessage()
                : fieldErrors.get(0).message();
        return ResponseEntity.badRequest().body(ApiErrorResponse.withFieldErrors(
                ApiErrorCode.VALIDATION_FAILED.name(), message, request.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException exception,
                                                             HttpServletRequest request) {
        log.debug("Malformed request body on {}: {}", request.getRequestURI(), exception.getMessage());
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                ApiErrorCode.MALFORMED_REQUEST.name(),
                "The request body is malformed. Check the JSON syntax and the allowed enum values.",
                request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
                                                               HttpServletRequest request) {
        String message = "Parameter '" + exception.getName() + "' has an invalid value.";
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                ApiErrorCode.VALIDATION_FAILED.name(), message, request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception,
                                                                HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), exception.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiErrorResponse.of(
                ApiErrorCode.DATA_INTEGRITY_VIOLATION.name(),
                "The operation conflicts with existing data (duplicate value or a referenced record).",
                request.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException exception,
                                                             HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.of(
                ApiErrorCode.PRODUCT_NOT_FOUND.name(), "The requested resource does not exist.",
                request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected failure on {}", request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiErrorResponse.of(
                ApiErrorCode.INTERNAL_ERROR.name(),
                "An unexpected error occurred. Please retry; if it persists, contact support.",
                request.getRequestURI()));
    }

    private static ApiErrorResponse.FieldErrorDetail toFieldError(FieldError error) {
        return new ApiErrorResponse.FieldErrorDetail(error.getField(), error.getDefaultMessage());
    }

    private static String lastNode(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int index = path.lastIndexOf('.');
        return index < 0 ? path : path.substring(index + 1);
    }
}
