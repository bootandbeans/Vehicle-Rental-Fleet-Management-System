package com.example.vpoptimizer.optimization.model;

import com.example.vpoptimizer.exception.ApiErrorCode;
import com.example.vpoptimizer.exception.BusinessException;

/**
 * Safety limits of the optimization engine.
 *
 * <p>The engine is a bounded dynamic program, so every dimension that drives its cost has to be
 * capped explicitly instead of {@code OutOfMemoryError} being an option. The values come from
 * configuration ({@code vp-optimizer.optimization.*}) so an operator can tune them per deployment
 * without touching the algorithm.</p>
 *
 * @param defaultResultLimit             result limit used when the caller does not ask for one
 * @param maximumResultLimit             hard ceiling for a caller supplied result limit
 * @param maxSelectedProducts            maximum number of products that may participate
 * @param defaultMaxQuantityPerProduct   cap applied to products without an explicit maximum quantity
 * @param maxVpCapacity                  ceiling for the VP axis of the dynamic program
 */
public record OptimizationLimits(int defaultResultLimit,
                                 int maximumResultLimit,
                                 int maxSelectedProducts,
                                 int defaultMaxQuantityPerProduct,
                                 int maxVpCapacity) {

    public static final OptimizationLimits DEFAULT = new OptimizationLimits(3, 10, 25, 10, 20_000);

    public OptimizationLimits {
        if (defaultResultLimit < 1 || maximumResultLimit < 1 || maxSelectedProducts < 1
                || defaultMaxQuantityPerProduct < 1 || maxVpCapacity < 1) {
            throw new BusinessException(ApiErrorCode.VALIDATION_FAILED,
                    "Optimization limits must all be positive.");
        }
    }

    /** Effective limit: applies the default and enforces the configured ceiling. */
    public int effectiveResultLimit(int requested) {
        int limit = requested <= 0 ? defaultResultLimit : requested;
        return Math.min(limit, maximumResultLimit);
    }

    /** Rejects requests with too many products before any work is done. */
    public void checkProductCount(int count) {
        if (count > maxSelectedProducts) {
            throw new BusinessException(ApiErrorCode.ENGINE_LIMIT_EXCEEDED,
                    "At most " + maxSelectedProducts + " products can participate in one optimization, but "
                            + count + " were selected.");
        }
    }

    /** Rejects VP windows the dynamic program is not sized for. */
    public void checkVpRangeSupported(VpRange range) {
        if (range.maximumVp() > maxVpCapacity) {
            throw new BusinessException(ApiErrorCode.ENGINE_LIMIT_EXCEEDED,
                    "The acceptable VP window (" + range.minimumVp() + " - " + range.maximumVp()
                            + " VP) exceeds the supported maximum of " + maxVpCapacity
                            + " VP. Lower the target VP or the tolerance.");
        }
    }
}
