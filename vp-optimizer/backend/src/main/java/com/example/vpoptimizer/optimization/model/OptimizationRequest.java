package com.example.vpoptimizer.optimization.model;

import com.example.vpoptimizer.exception.ApiErrorCode;
import com.example.vpoptimizer.exception.BusinessException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Everything the optimization engine needs. Deliberately free of Spring, JPA and HTTP types so the
 * algorithm can be unit tested - and later replaced - in isolation.
 *
 * <p>Products that must not be used are simply absent from {@link #products}; the engine never
 * looks at the full catalogue.</p>
 */
public record OptimizationRequest(List<ProductOption> products,
                                  PricingContext pricing,
                                  int targetVp,
                                  ToleranceType toleranceType,
                                  BigDecimal toleranceValue,
                                  int resultLimit,
                                  OptimizationLimits limits) {

    public OptimizationRequest {
        products = products == null ? List.of() : List.copyOf(products);
        Objects.requireNonNull(pricing, "A pricing context is required.");
        Objects.requireNonNull(limits, "Optimization limits are required.");
        if (products.isEmpty()) {
            throw new BusinessException(ApiErrorCode.NO_PRODUCTS_SELECTED);
        }
        if (targetVp < 0) {
            throw new BusinessException(ApiErrorCode.INVALID_TARGET_VP);
        }
        if (toleranceType == null) {
            throw new BusinessException(ApiErrorCode.INVALID_TOLERANCE, "The tolerance type is required.");
        }
        if (toleranceValue == null || toleranceValue.signum() < 0) {
            throw new BusinessException(ApiErrorCode.INVALID_TOLERANCE);
        }
        if (resultLimit < 1) {
            throw new BusinessException(ApiErrorCode.INVALID_RESULT_LIMIT);
        }
    }

    /** Normalized acceptable VP window. */
    public VpRange vpRange() {
        return VpRange.of(targetVp, toleranceType, toleranceValue);
    }

    /** Result limit after applying defaults and ceilings. */
    public int effectiveLimit() {
        return limits.effectiveResultLimit(resultLimit);
    }

    public BigDecimal discountPercent() {
        return pricing.discountPercent();
    }

    public BigDecimal gstPercent() {
        return pricing.gstPercent();
    }

    public List<ProductOption> requiredProducts() {
        return products.stream().filter(ProductOption::required).toList();
    }
}
