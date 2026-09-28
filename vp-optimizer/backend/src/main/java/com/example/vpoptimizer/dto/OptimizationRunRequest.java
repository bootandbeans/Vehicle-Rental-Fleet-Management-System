package com.example.vpoptimizer.dto;

import com.example.vpoptimizer.optimization.model.ToleranceType;
import com.example.vpoptimizer.validation.ValidOptimizationRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


import java.math.BigDecimal;
import java.util.List;

/**
 * Payload of {@code POST /api/optimizations}.
 *
 * <p>{@code productIds} is the explicit list of products the user allowed - the engine never sees
 * the rest of the catalogue. {@code requiredProductIds} must be a subset of {@code productIds}.</p>
 */
@ValidOptimizationRequest
public record OptimizationRunRequest(

        // Deliberately not @NotEmpty: an empty selection must fail with the specific
        // NO_PRODUCTS_SELECTED code (see OptimizationService), not with a generic validation error.
        List<Long> productIds,

        List<Long> requiredProductIds,

        @NotNull(message = "Discount percentage is required.")
        @DecimalMin(value = "0.00", message = "Discount must not be negative.")
        @DecimalMax(value = "100.00", message = "Discount must not exceed 100 percent.")
        BigDecimal discountPercent,

        @NotNull(message = "GST percentage is required (use 0 for no GST).")
        @DecimalMin(value = "0.00", message = "GST must not be negative.")
        @DecimalMax(value = "100.00", message = "GST must not exceed 100 percent.")
        BigDecimal gstPercent,

        @NotNull(message = "Target VP is required.")
        @Min(value = 0, message = "Target VP must not be negative.")
        @Max(value = 10_000_000, message = "Target VP is unrealistically large.")
        Integer targetVp,

        @NotNull(message = "Tolerance type is required.")
        ToleranceType toleranceType,

        @NotNull(message = "Tolerance value is required (use 0 for an exact target).")
        @DecimalMin(value = "0.00", message = "Tolerance must not be negative.")
        BigDecimal toleranceValue,

        @Min(value = 1, message = "Result limit must be at least 1.")
        @Max(value = 50, message = "Result limit must not exceed 50.")
        Integer resultLimit,

        @Size(max = 150, message = "The session name must not exceed 150 characters.")
        String name) {

    public OptimizationRunRequest {
        productIds = productIds == null ? List.of() : List.copyOf(productIds);
        requiredProductIds = requiredProductIds == null ? List.of() : List.copyOf(requiredProductIds);
    }

    /** Result limit requested by the caller, or the configured default. */
    public int resultLimitOrDefault(int fallback) {
        return resultLimit == null ? fallback : resultLimit;
    }
}
