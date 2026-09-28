package com.example.vpoptimizer.dto;

import com.example.vpoptimizer.validation.ValidQuantityRange;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Create / update payload for a catalogue product. */
@ValidQuantityRange
public record ProductRequest(

        @NotBlank(message = "Product name is required.")
        @Size(max = 150, message = "Product name must not exceed 150 characters.")
        String name,

        @Size(max = 64, message = "SKU must not exceed 64 characters.")
        String sku,

        @Size(max = 500, message = "Description must not exceed 500 characters.")
        String description,

        @NotNull(message = "MRP is required.")
        @DecimalMin(value = "0.00", message = "MRP must not be negative.")
        @Digits(integer = 12, fraction = 2, message = "MRP supports at most 2 decimal places.")
        BigDecimal mrp,

        @NotNull(message = "Volume point is required.")
        @Min(value = 0, message = "Volume point must not be negative.")
        @Max(value = 1_000_000, message = "Volume point is unrealistically large.")
        Integer volumePoint,

        Long categoryId,

        @Min(value = 0, message = "Minimum quantity must not be negative.")
        @Max(value = 10_000, message = "Minimum quantity is unrealistically large.")
        Integer minQuantity,

        @Min(value = 1, message = "Maximum quantity must be at least 1.")
        @Max(value = 10_000, message = "Maximum quantity is unrealistically large.")
        Integer maxQuantity,

        Boolean active) {

    public ProductRequest {
        sku = blankToNull(sku);
        description = blankToNull(description);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
