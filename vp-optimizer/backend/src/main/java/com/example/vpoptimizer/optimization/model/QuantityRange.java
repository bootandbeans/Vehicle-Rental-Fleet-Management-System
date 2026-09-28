package com.example.vpoptimizer.optimization.model;

import com.example.vpoptimizer.exception.ApiErrorCode;
import com.example.vpoptimizer.exception.BusinessException;

/**
 * The quantity range actually used for a product during an optimization.
 *
 * <p>Rules (single source of truth, shared by the engine and the selection preview):</p>
 * <ul>
 *   <li>{@code ALLOWED}: {@code min = 0}, {@code max = maxQuantity} or the configured cap when the
 *       product is unbounded.</li>
 *   <li>{@code REQUIRED}: {@code min = max(1, minQuantity)} - a required product is always bought
 *       at least once - and the same {@code max} rule.</li>
 * </ul>
 */
public record QuantityRange(int minimumQuantity, int maximumQuantity) {

    public QuantityRange {
        if (minimumQuantity < 0) {
            throw new BusinessException(ApiErrorCode.INVALID_QUANTITY_RANGE, "Minimum quantity must not be negative.");
        }
        if (maximumQuantity < minimumQuantity) {
            throw new BusinessException(ApiErrorCode.INVALID_QUANTITY_RANGE,
                    "Maximum quantity must not be smaller than the minimum quantity.");
        }
    }

    /** Resolves the effective range of a product under the given engine limits. */
    public static QuantityRange resolve(ProductOption option, OptimizationLimits limits) {
        int minimum = option.required()
                ? Math.max(1, option.minQuantity() == null ? 1 : option.minQuantity())
                : 0;
        int maximum = option.maxQuantity() != null
                ? option.maxQuantity()
                : limits.defaultMaxQuantityPerProduct();
        if (maximum < minimum) {
            throw new BusinessException(ApiErrorCode.INVALID_QUANTITY_RANGE,
                    "Product '" + option.name() + "' allows at most " + maximum
                            + " unit(s) but is required at least " + minimum + " time(s).");
        }
        return new QuantityRange(minimum, maximum);
    }

    /** Units the optimizer may add on top of the mandatory minimum. */
    public int extraCapacity() {
        return maximumQuantity - minimumQuantity;
    }
}
