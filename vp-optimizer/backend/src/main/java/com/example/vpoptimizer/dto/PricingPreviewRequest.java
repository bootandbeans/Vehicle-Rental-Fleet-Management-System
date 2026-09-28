package com.example.vpoptimizer.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** Payload of {@code POST /api/pricing/preview}: price the current selection without optimizing. */
public record PricingPreviewRequest(

        // Deliberately not @NotEmpty: the service answers with the specific NO_PRODUCTS_SELECTED code.
        List<Long> productIds,

        List<Long> requiredProductIds,

        @NotNull(message = "Discount percentage is required.")
        @DecimalMin(value = "0.00", message = "Discount must not be negative.")
        @DecimalMax(value = "100.00", message = "Discount must not exceed 100 percent.")
        BigDecimal discountPercent,

        @NotNull(message = "GST percentage is required (use 0 for no GST).")
        @DecimalMin(value = "0.00", message = "GST must not be negative.")
        @DecimalMax(value = "100.00", message = "GST must not exceed 100 percent.")
        BigDecimal gstPercent) {

    public PricingPreviewRequest {
        productIds = productIds == null ? List.of() : List.copyOf(productIds);
        requiredProductIds = requiredProductIds == null ? List.of() : List.copyOf(requiredProductIds);
    }
}
