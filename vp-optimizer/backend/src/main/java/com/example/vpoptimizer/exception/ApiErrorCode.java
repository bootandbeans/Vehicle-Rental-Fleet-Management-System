package com.example.vpoptimizer.exception;

import org.springframework.http.HttpStatus;

/**
 * Stable, machine readable error codes returned by the API.
 *
 * <p>Every code carries the HTTP status that should be used when it surfaces through
 * {@link GlobalExceptionHandler}.</p>
 */
public enum ApiErrorCode {

    // --- request validation -------------------------------------------------
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "The request body could not be parsed."),
    NO_PRODUCTS_SELECTED(HttpStatus.BAD_REQUEST, "Please select at least one product."),
    INVALID_TARGET_VP(HttpStatus.BAD_REQUEST, "Target VP must be greater than or equal to zero."),
    INVALID_DISCOUNT(HttpStatus.BAD_REQUEST, "Discount must be between 0 and 100 percent."),
    INVALID_GST(HttpStatus.BAD_REQUEST, "GST must be between 0 and 100 percent."),
    INVALID_TOLERANCE(HttpStatus.BAD_REQUEST, "Tolerance must be greater than or equal to zero."),
    INVALID_RESULT_LIMIT(HttpStatus.BAD_REQUEST, "Result limit must be at least 1."),
    INVALID_SELECTION(HttpStatus.BAD_REQUEST, "The product selection is inconsistent."),
    INVALID_QUANTITY_RANGE(HttpStatus.BAD_REQUEST, "The product quantity range is invalid."),
    ENGINE_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "The optimization request exceeds the supported limits."),
    NO_SELECTABLE_PRODUCTS(HttpStatus.BAD_REQUEST, "None of the selected products can participate in an optimization."),

    // --- catalogue ----------------------------------------------------------
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found."),
    PRODUCT_INACTIVE(HttpStatus.CONFLICT, "The product is inactive and cannot be used."),
    PRODUCT_IN_USE(HttpStatus.CONFLICT, "The product is referenced by optimization history."),
    PRODUCT_SKU_ALREADY_EXISTS(HttpStatus.CONFLICT, "A product with this SKU already exists."),

    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Category not found."),
    CATEGORY_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "A category with this name already exists."),
    CATEGORY_IN_USE(HttpStatus.CONFLICT, "The category is still assigned to one or more products."),

    // --- history ------------------------------------------------------------
    OPTIMIZATION_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Optimization session not found."),

    // --- infrastructure -----------------------------------------------------
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "The operation violates a data constraint."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");

    private final HttpStatus status;
    private final String defaultMessage;

    ApiErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
